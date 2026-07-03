package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.dto.ClassifiedQuestion;
import com.cloudera.customerformhub.service.QuestionnaireParserService;
import com.cloudera.customerformhub.service.QuestionnaireParserService.ParsedQuestion;
import com.cloudera.customerformhub.service.QuestionClassifierService;
import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.service.FormQuestionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/questionnaire")
public class QuestionnaireUploadController {

    private final QuestionnaireParserService parserService;
    private final QuestionClassifierService classifierService;
    private final FormQuestionService formQuestionService;

    public QuestionnaireUploadController(QuestionnaireParserService parserService,
                                         QuestionClassifierService classifierService,
                                         FormQuestionService formQuestionService) {
        this.parserService = parserService;
        this.classifierService = classifierService;
        this.formQuestionService = formQuestionService;
    }

    // POST /api/questionnaire/parse  → upload only, parse, no classification
    @PostMapping("/parse")
    public List<ParsedQuestion> parse(@RequestParam("file") MultipartFile file) {
        return parserService.parse(file);
    }

    // POST /api/questionnaire/classify  → upload, parse, then classify each question by department
    @PostMapping("/classify")
    public List<ClassifiedQuestion> classify(@RequestParam("file") MultipartFile file) {
        // 1. Parse the questions out of the Excel
        List<ParsedQuestion> parsed = parserService.parse(file);

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

    // POST /api/questionnaire/import?ticketId=2  → upload, parse, classify, and save to the ticket
    @PostMapping("/import")
    public List<FormQuestion> importQuestions(@RequestParam("file") MultipartFile file,
                                              @RequestParam("ticketId") Long ticketId) {
        // 1. Parse the questions out of the Excel
        List<ParsedQuestion> parsed = parserService.parse(file);

        // 2. Classify all questions in one LLM call
        List<String> questionTexts = new ArrayList<>();
        for (ParsedQuestion q : parsed) {
            questionTexts.add(q.questionText());
        }
        List<String> departments = classifierService.classify(questionTexts);

        // 3. Turn each into a FormQuestion and save it under this ticket
        List<FormQuestion> saved = new ArrayList<>();
        for (int i = 0; i < parsed.size(); i++) {
            ParsedQuestion q = parsed.get(i);
            String dept = (i < departments.size()) ? departments.get(i) : "General";

            FormQuestion fq = new FormQuestion(
                    ticketId,               // which ticket
                    q.questionText(),       // question text
                    dept,                   // department from the LLM
                    "Needs Review",         // initial status
                    null,                   // riskLevel (not set yet)
                    q.section()             // use the questionnaire section as rowReference
            );
            saved.add(formQuestionService.saveQuestion(fq));
        }

        return saved;
    }
}