package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FormQuestionService {

    private final FormQuestionRepository repository;

    public FormQuestionService(FormQuestionRepository repository) {
        this.repository = repository;
    }

    // Get all questions
    public List<FormQuestion> getAllQuestions() {
        return repository.findAll();
    }

    // Get all questions for one ticket
    public List<FormQuestion> getQuestionsByTicket(Long ticketId) {
        return repository.findByTicketId(ticketId);
    }

    // Get questions for one ticket in a specific department
    public List<FormQuestion> getQuestionsByTicketAndDepartment(Long ticketId, String department) {
        return repository.findByTicketIdAndDepartment(ticketId, department);
    }

    // Get one question by id (null if not found)
    public FormQuestion getQuestionById(Long id) {
        return repository.findById(id).orElse(null);
    }

    // Create or update a question
    public FormQuestion saveQuestion(FormQuestion question) {
        return repository.save(question);
    }

    // Update only the status of a question (returns null if not found)
    public FormQuestion updateStatus(Long id, String status) {
        FormQuestion question = repository.findById(id).orElse(null);
        if (question == null) {
            return null;
        }
        question.setStatus(status);
        return repository.save(question);
    }

    // Partial update: only non-null fields are copied onto the existing row
    public FormQuestion updateQuestion(Long id, FormQuestion changes) {
        FormQuestion existing = repository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        if (changes.getQuestionText() != null) existing.setQuestionText(changes.getQuestionText());
        if (changes.getDepartment() != null) existing.setDepartment(changes.getDepartment());
        if (changes.getStatus() != null) existing.setStatus(changes.getStatus());
        if (changes.getRiskLevel() != null) existing.setRiskLevel(changes.getRiskLevel());
        if (changes.getRowReference() != null) existing.setRowReference(changes.getRowReference());
        // ticketId and createdAt are intentionally never overwritten
        return repository.save(existing);
    }
}