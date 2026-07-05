package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FinalAnswer;
import com.cloudera.customerformhub.repository.FinalAnswerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FinalAnswerService {

    private final FinalAnswerRepository repository;

    public FinalAnswerService(FinalAnswerRepository repository) {
        this.repository = repository;
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
    public FinalAnswer saveAnswer(FinalAnswer answer) {
        List<FinalAnswer> existing = repository.findByQuestionId(answer.getQuestionId());
        if (!existing.isEmpty()) {
            // Update the existing answer for this question
            FinalAnswer current = existing.get(0);
            if (answer.getAnswerText() != null) current.setAnswerText(answer.getAnswerText());
            if (answer.getSourceChunkId() != null) current.setSourceChunkId(answer.getSourceChunkId());
            if (answer.getIsEdited() != null) current.setIsEdited(answer.getIsEdited());
            if (answer.getSourceType() != null) current.setSourceType(answer.getSourceType());
            if (answer.getApprovalStatus() != null) current.setApprovalStatus(answer.getApprovalStatus());
            if (answer.getApprovedBy() != null) current.setApprovedBy(answer.getApprovedBy());
            return repository.save(current);
        }
        // No answer yet for this question → create a new one
        return repository.save(answer);
    }

    // Delete an answer by id
    public void deleteAnswer(Long id) {
        repository.deleteById(id);
    }
}