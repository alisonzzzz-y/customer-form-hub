package com.cloudera.customerformhub.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sme_request_question")
public class SmeRequestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long smeRequestId;      // which SME request this belongs to

    @Column(nullable = false)
    private Long questionId;        // which form question is included

    private String status;          // Pending / Returned
    private String includedReason;  // why it was routed to SME, e.g. "No source found"

    @Column(columnDefinition = "TEXT")
    private String returnedAnswer;  // the answer the SME provided (null until returned)

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = "Pending";
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // No-arg constructor (required by JPA)
    public SmeRequestQuestion() {
    }

    // Convenience constructor
    public SmeRequestQuestion(Long smeRequestId, Long questionId, String status,
                              String includedReason, String returnedAnswer) {
        this.smeRequestId = smeRequestId;
        this.questionId = questionId;
        this.status = status;
        this.includedReason = includedReason;
        this.returnedAnswer = returnedAnswer;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSmeRequestId() { return smeRequestId; }
    public void setSmeRequestId(Long smeRequestId) { this.smeRequestId = smeRequestId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIncludedReason() { return includedReason; }
    public void setIncludedReason(String includedReason) { this.includedReason = includedReason; }

    public String getReturnedAnswer() { return returnedAnswer; }
    public void setReturnedAnswer(String returnedAnswer) { this.returnedAnswer = returnedAnswer; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}