package com.cloudera.customerformhub.repository;

import com.cloudera.customerformhub.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // The ticket a browser already created with this reference, if any
    Ticket findByClientRef(String clientRef);
}
