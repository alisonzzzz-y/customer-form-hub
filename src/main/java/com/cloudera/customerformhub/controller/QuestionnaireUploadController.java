package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.dto.ClassifiedQuestion;
import com.cloudera.customerformhub.service.DocxParserService;
import com.cloudera.customerformhub.service.QuestionnaireParserService;
import com.cloudera.customerformhub.service.QuestionnaireParserService.ParsedQuestion;
import com.cloudera.customerformhub.service.QuestionClassifierService;
import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.service.QuestionnaireImportService;
import com.cloudera.customerformhub.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/questionnaire")
public class QuestionnaireUploadController {

    private final QuestionnaireParserService parserService;
    private final DocxParserService docxParserService;
    private final QuestionClassifierService classifierService;
    private final QuestionnaireImportService importService;
    private final TicketService ticketService;

    public QuestionnaireUploadController(QuestionnaireParserService parserService,
                                         DocxParserService docxParserService,
                                         QuestionClassifierService classifierService,
                                         QuestionnaireImportService importService,
                                         TicketService ticketService) {
        this.parserService = parserService;
        this.docxParserService = docxParserService;
        this.classifierService = classifierService;
        this.importService = importService;
        this.ticketService = ticketService;
    }

    // POST /api/questionnaire/parse  → upload only, parse, no classification
    @PostMapping("/parse")
    public List<ParsedQuestion> parse(@RequestParam("file") MultipartFile file) {
        return parseAny(file);
    }

    // POST /api/questionnaire/classify  → upload, parse, then classify each question by department
    @PostMapping("/classify")
    public List<ClassifiedQuestion> classify(@RequestParam("file") MultipartFile file) {
        // 1. Parse the questions out of the Excel
        List<ParsedQuestion> parsed = parseAny(file);

        // 2. Pull just the question texts into a list for the classifier
        List<String> questionTexts = new ArrayList<>();
        for (ParsedQuestion q : parsed) {
            questionTexts.add(q.questionText());
        }

        // 3. Classify all questions in one LLM call → list of departments (index-aligned)
        List<String> departments = classifierService.classify(questionTexts);

        // 4. Combine parsed question + its department into the result DTO
        List<ClassifiedQuestion> result = new ArrayList<>();
        for (int i = 0; i < parsed.size(); i++) {
            ParsedQuestion q = parsed.get(i);
            String dept = (i < departments.size()) ? departments.get(i) : "General";
            result.add(new ClassifiedQuestion(q.section(), q.questionText(), dept));
        }

        return result;
    }

    // POST /api/questionnaire/import?ticketId=2  → upload, parse, classify, and save to the ticket.
    // The file is always parsed. Submitting the same file again inserts nothing;
    // a different file is reconciled as a new version of the form (see
    // FormQuestionService.importParsedQuestions). The ticket records the file
    // name, hash and outcome, which GET /api/tickets/{id} returns.
    @PostMapping("/import")
    public List<FormQuestion> importQuestions(@RequestParam("file") MultipartFile file,
                                              @RequestParam("ticketId") Long ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        if (ticket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket " + ticketId + " does not exist.");
        }

        List<ParsedQuestion> parsed = parseAny(file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return importService.importFile(ticket, file.getOriginalFilename(), bytes, parsed,
                classifierService::classify);
    }

    // Parse either Excel or Word questionnaire files into the same ParsedQuestion shape.
    private List<ParsedQuestion> parseAny(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.toLowerCase();

        if (lowerName.endsWith(".docx")) {
            String rawText = docxParserService.extractText(file);
            return classifierService.extractQuestions(rawText);
        }

        if (lowerName.endsWith(".xlsx")) {
            return parserService.parse(file);
        }

        throw new IllegalArgumentException("Unsupported file type: please upload .xlsx or .docx");
    }
}