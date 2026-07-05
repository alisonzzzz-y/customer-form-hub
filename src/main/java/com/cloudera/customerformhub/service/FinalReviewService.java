package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.QuestionWithAnswer;
import com.cloudera.customerformhub.entity.FinalAnswer;
import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FinalAnswerRepository;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FinalReviewService {

    private final FormQuestionRepository questionRepository;
    private final FinalAnswerRepository answerRepository;

    public FinalReviewService(FormQuestionRepository questionRepository,
                              FinalAnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    // Build the full review for a ticket: every question with its final answer (if any)
    public List<QuestionWithAnswer> getReviewForTicket(Long ticketId) {
        List<FormQuestion> questions = questionRepository.findByTicketId(ticketId);

        return questions.stream()
                .map(this::toQuestionWithAnswer)
                .toList();
    }

    // Combine one question with its final answer into a single DTO
    private QuestionWithAnswer toQuestionWithAnswer(FormQuestion q) {
        QuestionWithAnswer dto = new QuestionWithAnswer();

        // question fields
        dto.setQuestionId(q.getId());
        dto.setQuestionText(q.getQuestionText());
        dto.setDepartment(q.getDepartment());
        dto.setQuestionStatus(q.getStatus());
        dto.setRiskLevel(q.getRiskLevel());

        // look up the final answer for this question
        List<FinalAnswer> answers = answerRepository.findByQuestionId(q.getId());
        if (!answers.isEmpty()) {
            FinalAnswer a = answers.get(0);
            dto.setAnswerId(a.getId());
            dto.setAnswerText(a.getAnswerText());
            dto.setIsEdited(a.getIsEdited());
            dto.setSourceType(a.getSourceType());
            dto.setApprovedBy(a.getApprovedBy());
            dto.setApprovalStatus(a.getApprovalStatus());
            dto.setSourceChunkId(a.getSourceChunkId());
            dto.setAnswerUpdatedAt(a.getUpdatedAt());
            dto.setAnswered(true);
        } else {
            dto.setAnswered(false);
        }

        return dto;
    }
}