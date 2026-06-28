package com.cloudera.customerformhub.repository;

import com.cloudera.customerformhub.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}