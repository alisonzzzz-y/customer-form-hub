package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.service.QuestionnaireParserService.ParsedQuestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    // Get the questions of one ticket's current form (retired versions left out)
    public List<FormQuestion> getQuestionsByTicket(Long ticketId) {
        return repository.findByTicketIdAndRetiredAtIsNull(ticketId);
    }

    // Get questions for one ticket in a specific department
    public List<FormQuestion> getQuestionsByTicketAndDepartment(Long ticketId, String department) {
        return repository.findByTicketIdAndDepartmentAndRetiredAtIsNull(ticketId, department);
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
        if (changes.getDepartmentSource() != null) existing.setDepartmentSource(changes.getDepartmentSource());
        // ticketId and createdAt are intentionally never overwritten
        return repository.save(existing);
    }

    // Outcome of reconciling one customer form with the ticket's saved rows.
    public record ImportResult(List<FormQuestion> questions, int added, int retired) {
    }

    // Save parsed questionnaire rows under a ticket without duplicating rows an
    // earlier import already created. A row is matched by sheet/section and
    // question text, one-for-one, so a form that really repeats a question keeps
    // both rows. Matched rows are returned unchanged: reviewer edits, answers and
    // SME links are never overwritten. Only new rows are classified.
    //
    // When the file is a different version (retireMissing = true), rows the new
    // file no longer contains are retired, not deleted, and a retired row comes
    // back if a later version contains it again. A question whose wording
    // changed counts as removed + added, so an answer approved for the old
    // wording is never silently attached to the new one.
    //
    // A file that yields no questions is rejected before anything is changed:
    // treating it as a new version would retire every saved question.
    @Transactional
    public ImportResult importParsedQuestions(Long ticketId, List<ParsedQuestion> parsed,
                                              Function<List<String>, List<String>> classifier,
                                              boolean retireMissing) {
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("No questions were found in this file. Check that the sheet "
                    + "has a \"Question\" column with filled rows. The ticket's saved questions were not changed.");
        }
        // Active rows are matched first, so a retired copy is only revived when
        // the new file contains more copies of a question than are active.
        List<FormQuestion> saved = new ArrayList<>(repository.findByTicketId(ticketId));
        saved.sort(Comparator.comparing((FormQuestion q) -> q.getRetiredAt() != null)
                .thenComparing(FormQuestion::getId));
        Map<String, Deque<FormQuestion>> savedByKey = new HashMap<>();
        for (FormQuestion existing : saved) {
            savedByKey
                    .computeIfAbsent(importKey(existing.getRowReference(), existing.getQuestionText()),
                            key -> new ArrayDeque<>())
                    .add(existing);
        }

        FormQuestion[] result = new FormQuestion[parsed.size()];
        Set<Long> matchedIds = new HashSet<>();
        List<Integer> newIndexes = new ArrayList<>();
        for (int i = 0; i < parsed.size(); i++) {
            ParsedQuestion q = parsed.get(i);
            Deque<FormQuestion> matches = savedByKey.get(importKey(q.section(), q.questionText()));
            if (matches != null && !matches.isEmpty()) {
                FormQuestion match = matches.poll();
                if (match.getRetiredAt() != null) {
                    match.setRetiredAt(null);
                    match = repository.save(match);
                }
                matchedIds.add(match.getId());
                result[i] = match;
            } else {
                newIndexes.add(i);
            }
        }

        int retired = 0;
        if (retireMissing) {
            LocalDateTime now = LocalDateTime.now();
            for (FormQuestion existing : saved) {
                if (existing.getRetiredAt() == null && !matchedIds.contains(existing.getId())) {
                    existing.setRetiredAt(now);
                    repository.save(existing);
                    retired++;
                }
            }
        }

        if (!newIndexes.isEmpty()) {
            List<String> texts = newIndexes.stream().map(i -> parsed.get(i).questionText()).toList();
            List<String> departments = classifier.apply(texts);
            for (int n = 0; n < newIndexes.size(); n++) {
                int i = newIndexes.get(n);
                ParsedQuestion q = parsed.get(i);
                String dept = (n < departments.size()) ? departments.get(n) : "General";
                FormQuestion created = new FormQuestion(
                        ticketId, q.questionText(), dept, "Needs Review", null, q.section());
                created.setDepartmentSource("AI");
                result[i] = repository.save(created);
            }
        }

        return new ImportResult(Arrays.asList(result), newIndexes.size(), retired);
    }

    // Case- and whitespace-insensitive identity of one questionnaire row.
    static String importKey(String section, String questionText) {
        return normalise(section) + "\u0000" + normalise(questionText);
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
