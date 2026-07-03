package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.service.QuestionnaireParserService;
import com.cloudera.customerformhub.service.QuestionnaireParserService.ParsedQuestion;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/questionnaire")
public class QuestionnaireUploadController {

    private final QuestionnaireParserService parserService;

    public QuestionnaireUploadController(QuestionnaireParserService parserService) {
        this.parserService = parserService;
    }

    // POST /api/questionnaire/parse  → upload an .xlsx and get back the parsed questions
    // (Step 1: parse only. Classification and saving come later.)
    @PostMapping("/parse")
    public List<ParsedQuestion> parse(@RequestParam("file") MultipartFile file) {
        return parserService.parse(file);
    }
}