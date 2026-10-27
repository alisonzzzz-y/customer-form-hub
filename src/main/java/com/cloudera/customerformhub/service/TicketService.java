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

    private static final Set<String> VALID_STAGES = Set.of(
            "intake", "grouping", "review", "sme", "eta", "final", "done"
    );

    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    // Get all tickets
    public List<Ticket> getAllTickets() {
        // Reads are deliberately tolerant. Historical or manually-created
        // values must not make the whole ticket list fail.
        return repository.findAll();
    }

    // Get one ticket by id (returns null if not found)
    public Ticket getTicketById(Long id) {
        return repository.findById(id).orElse(null);
    }

    // Create a ticket. A create that repeats an earlier clientRef (a retry after
    // the first response was lost) returns the ticket already saved for it.
    public Ticket createTicket(Ticket ticket) {
        if (ticket.getClientRef() != null && !ticket.getClientRef().isBlank()) {
            Ticket existing = repository.findByClientRef(ticket.getClientRef());
            if (existing != null) return existing;
        }
        ticket.setId(null);
        ticket.setWorkflowStage(normaliseStage(ticket.getWorkflowStage()));
        // Form import details are written by the import only.
        ticket.setFormFileName(null);
        ticket.setFormFileHash(null);
        ticket.setFormImportedAt(null);
        ticket.setFormImportOutcome(null);
        ticket.setFormQuestionsAdded(null);
        ticket.setFormQuestionsRetired(null);
        return saveTicket(ticket);
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
        if (changes.getWorkflowStage() != null) existing.setWorkflowStage(normaliseStage(changes.getWorkflowStage()));
        // createdAt is intentionally never overwritten
        return repository.save(existing);
    }

    static String normaliseStage(String stage) {
        if (stage == null || stage.isBlank()) return null;
        if (!VALID_STAGES.contains(stage)) {
            throw new IllegalArgumentException("Invalid workflow stage: " + stage);
        }
        return stage;
    }

    public static String normaliseStatus(String status) {
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
