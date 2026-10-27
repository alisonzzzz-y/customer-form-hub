package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.service.QuestionnaireParserService.ParsedQuestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

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

    // Save parsed questionnaire rows under a ticket without duplicating rows that
    // an earlier import of the same file already created. Matching rows are
    // returned unchanged, so reviewer edits (department, status, answers) are
    // never overwritten and nothing is deleted. Only new rows are classified.
    // Identical questions inside one file are matched one-for-one, so a customer
    // form that really repeats a question keeps both rows.
    @Transactional
    public List<FormQuestion> importParsedQuestions(Long ticketId, List<ParsedQuestion> parsed,
                                                    Function<List<String>, List<String>> classifier) {
        Map<String, Deque<FormQuestion>> existingByKey = new HashMap<>();
        for (FormQuestion existing : repository.findByTicketId(ticketId)) {
            existingByKey
                    .computeIfAbsent(importKey(existing.getRowReference(), existing.getQuestionText()),
                            key -> new ArrayDeque<>())
                    .add(existing);
        }

        FormQuestion[] result = new FormQuestion[parsed.size()];
        List<Integer> newIndexes = new ArrayList<>();
        for (int i = 0; i < parsed.size(); i++) {
            ParsedQuestion q = parsed.get(i);
            Deque<FormQuestion> matches = existingByKey.get(importKey(q.section(), q.questionText()));
            if (matches != null && !matches.isEmpty()) {
                result[i] = matches.poll();
            } else {
                newIndexes.add(i);
            }
        }

        if (!newIndexes.isEmpty()) {
            List<String> texts = newIndexes.stream().map(i -> parsed.get(i).questionText()).toList();
            List<String> departments = classifier.apply(texts);
            for (int n = 0; n < newIndexes.size(); n++) {
                int i = newIndexes.get(n);
                ParsedQuestion q = parsed.get(i);
                String dept = (n < departments.size()) ? departments.get(n) : "General";
                result[i] = repository.save(new FormQuestion(
                        ticketId, q.questionText(), dept, "Needs Review", null, q.section()));
            }
        }

        return Arrays.asList(result);
    }

    // Case- and whitespace-insensitive identity of one questionnaire row.
    static String importKey(String section, String questionText) {
        return normalise(section) + "\u0000" + normalise(questionText);
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
