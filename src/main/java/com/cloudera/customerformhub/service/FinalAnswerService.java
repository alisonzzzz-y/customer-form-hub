package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.FinalAnswer;
import com.cloudera.customerformhub.repository.FinalAnswerRepository;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cloudera.customerformhub.repository.KnowledgeBaseRepository;
import com.cloudera.customerformhub.repository.TicketRepository;

import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class FinalAnswerService {

    private final FinalAnswerRepository repository;
    private final FormQuestionRepository questionRepository;

    private final KnowledgeBaseRepository knowledge;
    private final TicketRepository tickets;

    public FinalAnswerService(FinalAnswerRepository repository, FormQuestionRepository questionRepository,
                              KnowledgeBaseRepository knowledge, TicketRepository tickets) {
        this.knowledge = knowledge;
        this.tickets = tickets;
        this.repository = repository;
        this.questionRepository = questionRepository;
    }

    // Get all final answers
    public List<FinalAnswer> getAllAnswers() {
        return repository.findAll();
    }

    // Get one answer by id (null if not found)
    public FinalAnswer getAnswerById(Long id) {
        return repository.findById(id).orElse(null);
    }

    // Get the final answer for a question (null if none yet)
    public FinalAnswer getAnswerByQuestion(Long questionId) {
        List<FinalAnswer> answers = repository.findByQuestionId(questionId);
        if (answers.isEmpty()) {
            return null;
        }
        return answers.get(0);  // MVP: one answer per question
    }

    // Save (create or update) the final answer for a question.
    // If the question already has an answer, update it instead of creating a duplicate.
    @Transactional
    public FinalAnswer saveAnswer(FinalAnswer answer) {
        List<FinalAnswer> existing = repository.findByQuestionId(answer.getQuestionId());
        FinalAnswer previous = existing.isEmpty() ? null : existing.get(0);
        String approval = answer.getApprovalStatus() != null ? answer.getApprovalStatus()
                : previous == null ? null : previous.getApprovalStatus();
        String sourceType = answer.getSourceType() != null ? answer.getSourceType()
                : previous == null ? null : previous.getSourceType();
        boolean independentAnswer = "SME".equals(sourceType) || "Manual".equals(sourceType);
        Long sourceId = independentAnswer ? null : answer.getSourceChunkId() != null
                ? answer.getSourceChunkId() : previous == null ? null : previous.getSourceChunkId();
        if ("Confirmed".equals(approval) && sourceId != null) {
            var source = knowledge.findById(sourceId).orElse(null);
            if (source != null && source.getSharingStatus() != null &&
                    source.getSharingStatus().replace(' ', '-').equalsIgnoreCase("NDA-required")) {
                var question = questionRepository.findById(answer.getQuestionId()).orElseThrow(
                        () -> new IllegalArgumentException("Question not found"));
                var ticket = tickets.findById(question.getTicketId()).orElseThrow(
                        () -> new IllegalArgumentException("Ticket not found"));
                if (!"Yes".equalsIgnoreCase(ticket.getNdaStatus()) &&
                        !"In Place".equalsIgnoreCase(ticket.getNdaStatus())) {
                    throw new IllegalArgumentException("An NDA must be in place before approving this knowledge answer, including edited answers.");
                }
            }
        }
        FinalAnswer saved;
        if (!existing.isEmpty()) {
            // Update the existing answer for this question
            FinalAnswer current = existing.get(0);
            if (answer.getAnswerText() != null) current.setAnswerText(answer.getAnswerText());
            if (independentAnswer) current.setSourceChunkId(null);
            else if (answer.getSourceChunkId() != null) current.setSourceChunkId(answer.getSourceChunkId());
            if (answer.getIsEdited() != null) current.setIsEdited(answer.getIsEdited());
            if (answer.getSourceType() != null) current.setSourceType(answer.getSourceType());
            if (answer.getApprovalStatus() != null) current.setApprovalStatus(answer.getApprovalStatus());
            if (answer.getApprovedBy() != null) current.setApprovedBy(answer.getApprovedBy());
            saved = repository.save(current);
        } else {
            // No answer yet for this question → create a new one
            if (independentAnswer) answer.setSourceChunkId(null);
            saved = repository.save(answer);
        }

        // Keep the related question status in sync when the final answer is confirmed.
        if ("Confirmed".equals(saved.getApprovalStatus())) {
            questionRepository.findById(saved.getQuestionId()).ifPresent(question -> {
                if (!"Answered".equals(question.getStatus())) {
                    question.setStatus("Answered");
                }
                question.setAiSuggestionSourceId(saved.getSourceChunkId());
                if (saved.getSourceChunkId() != null && "Knowledge Base".equals(saved.getSourceType())) {
                    question.setReviewOutcome(Boolean.TRUE.equals(saved.getIsEdited())
                            ? "EDITED" : "ACCEPTED");
                } else {
                    question.setReviewOutcome(null);
                }
                question.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
                questionRepository.save(question);
            });
        }

        return saved;
    }
}
