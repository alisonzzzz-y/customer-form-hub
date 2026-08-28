package com.cloudera.customerformhub.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "form_question")
public class FormQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;          // which ticket this question belongs to

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;    // the actual question from the customer form

    private String department;      // InfoSec / Legal / HR / Finance / ESG (classified)
    private String status;          // Needs Review / Source Found / SME Needed / Answered
    private String riskLevel;       // High / Medium / Low (optional)
    private String rowReference;    // e.g. the row in the original Excel, optional

    // Current AI-review state. V1 intentionally keeps only the latest outcome;
    // operational request timestamps remain available separately.
    private Long aiSuggestionSourceId;
    private String reviewOutcome;   // ACCEPTED / EDITED / ESCALATED
    private LocalDateTime reviewedAt;
    private LocalDateTime aeClarificationRequestedAt;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = "Needs Review";
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    // No-arg constructor (required by JPA)
    public FormQuestion() {
    }

    // Convenience constructor for seeding
    public FormQuestion(Long ticketId, String questionText, String department,
                        String status, String riskLevel, String rowReference) {
        this.ticketId = ticketId;
        this.questionText = questionText;
        this.department = department;
        this.status = status;
        this.riskLevel = riskLevel;
        this.rowReference = rowReference;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getRowReference() { return rowReference; }
    public void setRowReference(String rowReference) { this.rowReference = rowReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getAiSuggestionSourceId() { return aiSuggestionSourceId; }
    public void setAiSuggestionSourceId(Long aiSuggestionSourceId) { this.aiSuggestionSourceId = aiSuggestionSourceId; }

    public String getReviewOutcome() { return reviewOutcome; }
    public void setReviewOutcome(String reviewOutcome) { this.reviewOutcome = reviewOutcome; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getAeClarificationRequestedAt() { return aeClarificationRequestedAt; }
    public void setAeClarificationRequestedAt(LocalDateTime aeClarificationRequestedAt) {
        this.aeClarificationRequestedAt = aeClarificationRequestedAt;
    }
}
