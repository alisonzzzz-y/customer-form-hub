package com.cloudera.customerformhub.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "final_answer")
public class FinalAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long questionId;        // which form question this answer belongs to

    private Long sourceChunkId;     // which knowledge base chunk it was based on (optional)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerText;      // the final, human-reviewed answer text

    private Boolean isEdited;       // true if the analyst edited the retrieved text
    private String sourceType;      // e.g. "Knowledge Base" / "SME" / "Manual"
    private String approvalStatus;  // Draft / Confirmed
    private String approvedBy;      // who confirmed it (analyst name; string for MVP)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.approvalStatus == null) this.approvalStatus = "Confirmed";
        if (this.isEdited == null) this.isEdited = false;
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // No-arg constructor (required by JPA)
    public FinalAnswer() {
    }

    // Convenience constructor
    public FinalAnswer(Long questionId, Long sourceChunkId, String answerText,
                       Boolean isEdited, String sourceType, String approvalStatus, String approvedBy) {
        this.questionId = questionId;
        this.sourceChunkId = sourceChunkId;
        this.answerText = answerText;
        this.isEdited = isEdited;
        this.sourceType = sourceType;
        this.approvalStatus = approvalStatus;
        this.approvedBy = approvedBy;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Long getSourceChunkId() { return sourceChunkId; }
    public void setSourceChunkId(Long sourceChunkId) { this.sourceChunkId = sourceChunkId; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}