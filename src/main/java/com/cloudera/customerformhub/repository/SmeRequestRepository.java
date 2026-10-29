package com.cloudera.customerformhub.repository;

import com.cloudera.customerformhub.entity.SmeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmeRequestRepository extends JpaRepository<SmeRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SmeRequest r where r.id = :id")
    Optional<SmeRequest> findForPackaging(@Param("id") Long id);

    // Find all SME requests belonging to a given ticket
    List<SmeRequest> findByTicketId(Long ticketId);
}