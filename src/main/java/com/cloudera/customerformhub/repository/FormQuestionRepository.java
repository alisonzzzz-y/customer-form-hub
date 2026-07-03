package com.cloudera.customerformhub.repository;

import com.cloudera.customerformhub.entity.FormQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormQuestionRepository extends JpaRepository<FormQuestion, Long> {

    // Find all questions belonging to a given ticket
    List<FormQuestion> findByTicketId(Long ticketId);

    // Find all questions of a ticket in a given department (useful for SME packaging later)
    List<FormQuestion> findByTicketIdAndDepartment(Long ticketId, String department);
}