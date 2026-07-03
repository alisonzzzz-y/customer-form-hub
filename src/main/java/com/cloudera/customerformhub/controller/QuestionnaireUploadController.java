package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.dto.ClassifiedQuestion;
import com.cloudera.customerformhub.service.QuestionnaireParserService;
import com.cloudera.customerformhub.service.QuestionnaireParserService.ParsedQuestion;
import com.cloudera.customerformhub.service.QuestionClassifierService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/questionnaire")
public class QuestionnaireUploadController {

    private final QuestionnaireParserService parserService;
    private final QuestionClassifierService classifierService;

    public QuestionnaireUploadController(QuestionnaireParserService parserService,
                                         QuestionClassifierService classifierService) {
        this.parserService = parserService;
        this.classifierService = classifierService;
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
}