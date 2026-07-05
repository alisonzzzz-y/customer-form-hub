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

    // Update only the status of a ticket (returns null if not found)
    public Ticket updateStatus(Long id, String status) {
        Ticket ticket = repository.findById(id).orElse(null);
        if (ticket == null) {
            return null;
        }
        ticket.setStatus(status);
        return repository.save(ticket);
    }

    // Partial update: copy only the non-null fields from the request onto the
    // existing row, so a client that omits a field can never wipe it to null.
    public Ticket updateTicket(Long id, Ticket changes) {
        Ticket existing = repository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        if (changes.getCustomerName() != null) existing.setCustomerName(changes.getCustomerName());
        if (changes.getCreatedBy() != null) existing.setCreatedBy(changes.getCreatedBy());
        if (changes.getAssignedTo() != null) existing.setAssignedTo(changes.getAssignedTo());
        if (changes.getStatus() != null) existing.setStatus(changes.getStatus());
        if (changes.getUrgency() != null) existing.setUrgency(changes.getUrgency());
        if (changes.getNdaStatus() != null) existing.setNdaStatus(changes.getNdaStatus());
        if (changes.getDeadline() != null) existing.setDeadline(changes.getDeadline());
        if (changes.getBusinessImpact() != null) existing.setBusinessImpact(changes.getBusinessImpact());
        if (changes.getEta() != null) existing.setEta(changes.getEta());
        // createdAt is intentionally never overwritten
        return repository.save(existing);
    }
}