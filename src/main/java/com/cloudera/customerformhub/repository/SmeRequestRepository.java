package com.cloudera.customerformhub.repository;

import com.cloudera.customerformhub.entity.SmeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmeRequestRepository extends JpaRepository<SmeRequest, Long> {

    // Find all SME requests belonging to a given ticket
    List<SmeRequest> findByTicketId(Long ticketId);
}