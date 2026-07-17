package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class TicketService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "New", "AI Processing", "Intake Review", "In Progress",
            "Waiting SME", "Ready for Review", "Approved", "Sent",
            "Closed", "Archived"
    );

    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    // Get all tickets
    public List<Ticket> getAllTickets() {
        return repository.findAll().stream().map(this::normaliseStoredStatus).toList();
    }

    // Get one ticket by id (returns null if not found)
    public Ticket getTicketById(Long id) {
        Ticket ticket = repository.findById(id).orElse(null);
        return ticket == null ? null : normaliseStoredStatus(ticket);
    }

    // Create or update a ticket
    public Ticket saveTicket(Ticket ticket) {
        ticket.setStatus(normaliseStatus(ticket.getStatus()));
        return repository.save(ticket);
    }

    // Update only the status of a ticket (returns null if not found)
    public Ticket updateStatus(Long id, String status) {
        Ticket ticket = repository.findById(id).orElse(null);
        if (ticket == null) {
            return null;
        }
        ticket.setStatus(normaliseStatus(status));
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
        if (changes.getStatus() != null) existing.setStatus(normaliseStatus(changes.getStatus()));
        if (changes.getUrgency() != null) existing.setUrgency(changes.getUrgency());
        if (changes.getNdaStatus() != null) existing.setNdaStatus(changes.getNdaStatus());
        if (changes.getDeadline() != null) existing.setDeadline(changes.getDeadline());
        if (changes.getBusinessImpact() != null) existing.setBusinessImpact(changes.getBusinessImpact());
        if (changes.getEta() != null) existing.setEta(changes.getEta());
        // createdAt is intentionally never overwritten
        return repository.save(existing);
    }

    private Ticket normaliseStoredStatus(Ticket ticket) {
        String normalised = normaliseStatus(ticket.getStatus());
        if (!normalised.equals(ticket.getStatus())) {
            ticket.setStatus(normalised);
            return repository.save(ticket);
        }
        return ticket;
    }

    static String normaliseStatus(String status) {
        if (status == null || status.isBlank()) return "New";

        // One-time compatibility for rows created by the earlier, smaller model.
        String normalised = switch (status) {
            case "Intake Missing" -> "Intake Review";
            case "In Review" -> "In Progress";
            case "Completed" -> "Closed";
            default -> status;
        };

        if (!VALID_STATUSES.contains(normalised)) {
            throw new IllegalArgumentException("Invalid ticket status: " + status);
        }
        return normalised;
    }
}
