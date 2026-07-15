package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.entity.FinalAnswer;
import com.cloudera.customerformhub.service.FinalAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/final-answers")
public class FinalAnswerController {

    private final FinalAnswerService answerService;

    public FinalAnswerController(FinalAnswerService answerService) {
        this.answerService = answerService;
    }

    // GET /api/final-answers  → all answers
    @GetMapping
    public List<FinalAnswer> getAll() {
        return answerService.getAllAnswers();
    }

    // GET /api/final-answers/question/{questionId}  → the final answer for a question
    @GetMapping("/question/{questionId}")
    public ResponseEntity<FinalAnswer> getByQuestion(@PathVariable Long questionId) {
        FinalAnswer answer = answerService.getAnswerByQuestion(questionId);
        if (answer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(answer);
    }

    // GET /api/final-answers/{id}  → one answer
    @GetMapping("/{id}")
    public ResponseEntity<FinalAnswer> getById(@PathVariable Long id) {
        FinalAnswer answer = answerService.getAnswerById(id);
        if (answer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(answer);
    }

    // POST /api/final-answers  → save a final answer (create or update for the question)
    @PostMapping
    public FinalAnswer save(@RequestBody FinalAnswer answer) {
        return answerService.saveAnswer(answer);
    }
}