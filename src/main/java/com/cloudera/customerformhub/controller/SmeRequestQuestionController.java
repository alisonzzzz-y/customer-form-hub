package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.entity.SmeRequestQuestion;
import com.cloudera.customerformhub.service.SmeRequestQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sme-request-questions")
public class SmeRequestQuestionController {

    private final SmeRequestQuestionService service;

    public SmeRequestQuestionController(SmeRequestQuestionService service) {
        this.service = service;
    }

    // GET /api/sme-request-questions/request/{smeRequestId}  → all questions in a request
    @GetMapping("/request/{smeRequestId}")
    public List<SmeRequestQuestion> getByRequest(@PathVariable Long smeRequestId) {
        return service.getByRequest(smeRequestId);
    }

    // GET /api/sme-request-questions/question/{questionId}  → SME records for a question
    @GetMapping("/question/{questionId}")
    public List<SmeRequestQuestion> getByQuestion(@PathVariable Long questionId) {
        return service.getByQuestion(questionId);
    }

    // GET /api/sme-request-questions/{id}  → one record
    @GetMapping("/{id}")
    public ResponseEntity<SmeRequestQuestion> getById(@PathVariable Long id) {
        SmeRequestQuestion item = service.getById(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    // POST /api/sme-request-questions  → create one link record manually
    @PostMapping
    public SmeRequestQuestion create(@RequestBody SmeRequestQuestion item) {
        return service.save(item);
    }

    // POST /api/sme-request-questions/package  → auto-package a ticket+department's SME-needed questions
    @PostMapping("/package")
    public List<SmeRequestQuestion> packageQuestions(@RequestBody Map<String, Object> body) {
        Long smeRequestId = Long.valueOf(body.get("smeRequestId").toString());
        Long ticketId = Long.valueOf(body.get("ticketId").toString());
        String department = body.get("department").toString();
        return service.packageQuestions(smeRequestId, ticketId, department);
    }

    // PATCH /api/sme-request-questions/{id}/answer  → record the SME's returned answer
    @PatchMapping("/{id}/answer")
    public ResponseEntity<SmeRequestQuestion> recordAnswer(@PathVariable Long id,
                                                           @RequestBody Map<String, String> body) {
        String answer = body.get("returnedAnswer");
        if (answer == null || answer.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        SmeRequestQuestion updated = service.recordAnswer(id, answer);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

}