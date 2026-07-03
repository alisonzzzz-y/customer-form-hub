package com.cloudera.customerformhub.repository;

import com.cloudera.customerformhub.entity.FinalAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinalAnswerRepository extends JpaRepository<FinalAnswer, Long> {

    // Find the final answer(s) for a given question
    List<FinalAnswer> findByQuestionId(Long questionId);
}