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
    private String status;      // New / Intake Missing / In Review / Waiting SME / Completed

    private String urgency;     // High / Medium / Low
    private String ndaStatus;   // Yes / No / Unknown

    private LocalDateTime deadline;
    private String businessImpact;

    private LocalDateTime eta;       // expected completion (full date-time, UTC)
    private LocalDateTime createdAt; // when the ticket was created

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
}