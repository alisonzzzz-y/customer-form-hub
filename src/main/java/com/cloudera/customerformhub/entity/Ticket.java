package com.cloudera.customerformhub.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    private String createdBy;   // the AE / requester, e.g. "Jane Smith"
    private String assignedTo;  // the GOM analyst owner, e.g. "Sarah"

    @Column(nullable = false)
    private String status;      // Full PRD lifecycle; validated by TicketService

    private String urgency;     // High / Medium / Low
    private String ndaStatus;   // Yes / No / Unknown

    private LocalDateTime deadline;
    private String businessImpact;

    private LocalDateTime eta;       // expected completion (full date-time, UTC)
    private LocalDateTime createdAt; // when the ticket was created
    private LocalDateTime closedAt;  // when it was last closed (UTC); cleared on reopen

    // Workflow step the user is on (intake / grouping / review / sme / eta /
    // final / done). Null for older rows, whose step is inferred from questions.
    private String workflowStage;

    // Browser-generated reference sent on create, so a retried create after a
    // lost response returns the same ticket instead of a second one.
    @Column(unique = true)
    private String clientRef;

    // Last customer form imported into this ticket. Set by the import only.
    private String formFileName;
    private String formFileHash;          // SHA-256 of the file bytes
    private LocalDateTime formImportedAt;
    private String formImportOutcome;     // CREATED / UNCHANGED / UPDATED
    private Integer formQuestionsAdded;   // new rows from the last import
    private Integer formQuestionsRetired; // rows no longer in the last file

    // Default values are set automatically before the row is first saved
    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = "New";
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    // No-arg constructor (required by JPA)
    public Ticket() {
    }

    // Convenience constructor for seeding data
    public Ticket(String customerName, String createdBy, String assignedTo, String status,
                  String urgency, String ndaStatus, LocalDateTime deadline,
                  String businessImpact, LocalDateTime eta, LocalDateTime createdAt) {
        this.customerName = customerName;
        this.createdBy = createdBy;
        this.assignedTo = assignedTo;
        this.status = status;
        this.urgency = urgency;
        this.ndaStatus = ndaStatus;
        this.deadline = deadline;
        this.businessImpact = businessImpact;
        this.eta = eta;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getNdaStatus() { return ndaStatus; }
    public void setNdaStatus(String ndaStatus) { this.ndaStatus = ndaStatus; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public String getBusinessImpact() { return businessImpact; }
    public void setBusinessImpact(String businessImpact) { this.businessImpact = businessImpact; }

    public LocalDateTime getEta() { return eta; }
    public void setEta(LocalDateTime eta) { this.eta = eta; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getWorkflowStage() { return workflowStage; }
    public void setWorkflowStage(String workflowStage) { this.workflowStage = workflowStage; }

    public String getClientRef() { return clientRef; }
    public void setClientRef(String clientRef) { this.clientRef = clientRef; }

    public String getFormFileName() { return formFileName; }
    public void setFormFileName(String formFileName) { this.formFileName = formFileName; }

    public String getFormFileHash() { return formFileHash; }
    public void setFormFileHash(String formFileHash) { this.formFileHash = formFileHash; }

    public LocalDateTime getFormImportedAt() { return formImportedAt; }
    public void setFormImportedAt(LocalDateTime formImportedAt) { this.formImportedAt = formImportedAt; }

    public String getFormImportOutcome() { return formImportOutcome; }
    public void setFormImportOutcome(String formImportOutcome) { this.formImportOutcome = formImportOutcome; }

    public Integer getFormQuestionsAdded() { return formQuestionsAdded; }
    public void setFormQuestionsAdded(Integer formQuestionsAdded) { this.formQuestionsAdded = formQuestionsAdded; }

    public Integer getFormQuestionsRetired() { return formQuestionsRetired; }
    public void setFormQuestionsRetired(Integer formQuestionsRetired) { this.formQuestionsRetired = formQuestionsRetired; }
}
