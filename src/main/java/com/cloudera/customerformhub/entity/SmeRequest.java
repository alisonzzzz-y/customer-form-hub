package com.cloudera.customerformhub.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sme_request")
public class SmeRequest {

    public static final String DRAFT_PREPARED = "Draft Prepared";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;        // which ticket this request belongs to

    @Column(nullable = false)
    private String department;    // InfoSec / Legal / HR / Finance / ESG

    private String teamName;      // e.g. "InfoSec Team"
    private Integer questionCount;

    private LocalDateTime eta;         // expected return time (full date-time, UTC)
    private String status;             // Draft Prepared / Waiting for ETA / ETA Confirmed / Overdue / Returned
    private String confirmedBy;        // who confirmed the ETA (optional)

    // When the request was sent. The app only opens a mail draft, so for a
    // "Draft Prepared" request this stays null until a user confirms the send.
    private LocalDateTime sentAt;
    private LocalDateTime returnedAt;  // when the SME actually returned it (nullable)

    // Set defaults before the row is first saved
    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = "Waiting for ETA";
        if (this.sentAt == null && !DRAFT_PREPARED.equals(this.status)) this.sentAt = LocalDateTime.now();
    }

    // No-arg constructor (required by JPA)
    public SmeRequest() {
    }

    // Convenience constructor for seeding
    public SmeRequest(Long ticketId, String department, String teamName, Integer questionCount,
                      LocalDateTime eta, String status, String confirmedBy,
                      LocalDateTime sentAt, LocalDateTime returnedAt) {
        this.ticketId = ticketId;
        this.department = department;
        this.teamName = teamName;
        this.questionCount = questionCount;
        this.eta = eta;
        this.status = status;
        this.confirmedBy = confirmedBy;
        this.sentAt = sentAt;
        this.returnedAt = returnedAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public LocalDateTime getEta() { return eta; }
    public void setEta(LocalDateTime eta) { this.eta = eta; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
}