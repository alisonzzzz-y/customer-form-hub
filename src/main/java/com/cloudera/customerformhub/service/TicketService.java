package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    // Get all tickets
    public List<Ticket> getAllTickets() {
        return repository.findAll();
    }

    // Get one ticket by id (returns null if not found)
    public Ticket getTicketById(Long id) {
        return repository.findById(id).orElse(null);
    }

    // Create or update a ticket
    public Ticket saveTicket(Ticket ticket) {
        return repository.save(ticket);
    }

    // Delete a ticket by id
    public void deleteTicket(Long id) {
        repository.deleteById(id);
    }
}