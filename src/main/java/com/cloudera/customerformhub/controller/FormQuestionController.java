package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.dto.SearchResult;
import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.service.FormQuestionService;
import com.cloudera.customerformhub.service.RetrievalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class FormQuestionController {

    private final FormQuestionService questionService;
    private final RetrievalService retrievalService;

    public FormQuestionController(FormQuestionService questionService, RetrievalService retrievalService) {
        this.questionService = questionService;
        this.retrievalService = retrievalService;
    }

    // GET /api/questions  → all questions
    @GetMapping
    public List<FormQuestion> getAll() {
        return questionService.getAllQuestions();
    }

    // GET /api/questions/ticket/{ticketId}  → all questions for one ticket
    @GetMapping("/ticket/{ticketId}")
    public List<FormQuestion> getByTicket(@PathVariable Long ticketId) {
        return questionService.getQuestionsByTicket(ticketId);
    }

    // GET /api/questions/ticket/{ticketId}/department/{department}  → questions in one department
    @GetMapping("/ticket/{ticketId}/department/{department}")
    public List<FormQuestion> getByTicketAndDepartment(@PathVariable Long ticketId,
                                                       @PathVariable String department) {
        return questionService.getQuestionsByTicketAndDepartment(ticketId, department);
    }

    // GET /api/questions/{id}  → one question
    @GetMapping("/{id}")
    public ResponseEntity<FormQuestion> getById(@PathVariable Long id) {
        FormQuestion question = questionService.getQuestionById(id);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(question);
    }

    // GET /api/questions/{id}/suggestions  → suggested knowledge base chunks for one question
    @GetMapping("/{id}/suggestions")
    public ResponseEntity<List<SearchResult>> getSuggestions(@PathVariable Long id) {
        FormQuestion question = questionService.getQuestionById(id);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        List<SearchResult> suggestions = retrievalService.search(question.getQuestionText());
        return ResponseEntity.ok(suggestions);
    }

    // POST /api/questions  → create a question
    @PostMapping
    public FormQuestion create(@RequestBody FormQuestion question) {
        return questionService.saveQuestion(question);
    }

    // PUT /api/questions/{id}  → update a question
    @PutMapping("/{id}")
    public ResponseEntity<FormQuestion> update(@PathVariable Long id, @RequestBody FormQuestion question) {
        FormQuestion updated = questionService.updateQuestion(id, question);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    // PATCH /api/questions/{id}/status  → update only the status
    @PatchMapping("/{id}/status")
    public ResponseEntity<FormQuestion> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        FormQuestion updated = questionService.updateStatus(id, status);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/questions/{id}  → delete a question
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        FormQuestion existing = questionService.getQuestionById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}