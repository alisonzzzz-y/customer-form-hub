package com.cloudera.customerformhub.repository;

import com.cloudera.customerformhub.entity.SmeRequestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmeRequestQuestionRepository extends JpaRepository<SmeRequestQuestion, Long> {

    // Find all questions included in a given SME request
    List<SmeRequestQuestion> findBySmeRequestId(Long smeRequestId);

    // Find the SME-request records for a given question
    List<SmeRequestQuestion> findByQuestionId(Long questionId);
}