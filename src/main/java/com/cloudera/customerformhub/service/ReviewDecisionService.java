package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.FinalAnswerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ReviewDecisionService {

    private final FormQuestionRepository questionRepository;

    private final FinalAnswerRepository answers;

    public ReviewDecisionService(FormQuestionRepository questionRepository, FinalAnswerRepository answers) {
        this.answers = answers;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public FormQuestion escalate(Long questionId, String type, Long suggestionSourceId) {
        FormQuestion question = questionRepository.findById(questionId).orElse(null);
        if (question == null) return null;

        String normalisedType = type == null ? "" : type.trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if ("SME".equals(normalisedType)) {
            if (!QuestionDepartments.isAssigned(question.getDepartment())) {
                throw new IllegalArgumentException("Assign an owning department in Grouping before routing to SME");
            }
            question.setStatus("SME Needed");
        } else if ("AE".equals(normalisedType)) {
            question.setStatus("Waiting AE");
            question.setAeClarificationRequestedAt(now);
        } else {
            throw new IllegalArgumentException("Escalation type must be SME or AE");
        }

        question.setAiSuggestionSourceId(suggestionSourceId);
        question.setReviewOutcome(suggestionSourceId == null ? null : "ESCALATED");
        question.setReviewedAt(now);
        return questionRepository.save(question);
    }

    @Transactional
    public FormQuestion reopen(Long questionId) {
        FormQuestion question = questionRepository.findById(questionId).orElse(null);
        if (question == null) return null;
        for (var answer : answers.findByQuestionId(questionId)) {
            answer.setApprovalStatus("Draft");
            answers.save(answer);
        }
        question.setStatus("Needs Review");
        question.setReviewOutcome(null);
        question.setReviewedAt(null);
        return questionRepository.save(question);
    }
}
