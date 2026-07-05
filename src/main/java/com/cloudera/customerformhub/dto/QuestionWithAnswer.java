package com.cloudera.customerformhub.dto;

import java.time.LocalDateTime;

public class QuestionWithAnswer {

    // Question fields
    private Long questionId;
    private String questionText;
    private String department;
    private String questionStatus;   // the FormQuestion status
    private String riskLevel;

    // Final answer fields (null if the question has no final answer yet)
    private Long answerId;
    private String answerText;
    private Boolean isEdited;
    private String sourceType;
    private String approvedBy;
    private String approvalStatus;        // Draft / Confirmed
    private Long sourceChunkId;           // traceability: which KB chunk it came from
    private LocalDateTime answerUpdatedAt; // freshness: when the answer was last updated
    private boolean answered;         // convenience flag: does this question have a final answer?

    // Getters and setters
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getQuestionStatus() { return questionStatus; }
    public void setQuestionStatus(String questionStatus) { this.questionStatus = questionStatus; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public Long getSourceChunkId() { return sourceChunkId; }
    public void setSourceChunkId(Long sourceChunkId) { this.sourceChunkId = sourceChunkId; }

    public LocalDateTime getAnswerUpdatedAt() { return answerUpdatedAt; }
    public void setAnswerUpdatedAt(LocalDateTime answerUpdatedAt) { this.answerUpdatedAt = answerUpdatedAt; }

    public boolean isAnswered() { return answered; }
    public void setAnswered(boolean answered) { this.answered = answered; }
}