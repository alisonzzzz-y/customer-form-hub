package com.cloudera.customerformhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "retrieval_evaluation_run")
public class RetrievalEvaluationRun {
    @Id
    @Column(length = 36)
    private String id;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private String datasetVersion;
    private String datasetHash;
    private Integer caseCount;
    private Integer failedCount;
    private Integer skippedCount;
    private Integer top1Hits;
    private Integer top3Hits;
    @Column(length = 500)
    private String errorMessage;

    public RetrievalEvaluationRun() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getDatasetVersion() { return datasetVersion; }
    public void setDatasetVersion(String datasetVersion) { this.datasetVersion = datasetVersion; }
    public String getDatasetHash() { return datasetHash; }
    public void setDatasetHash(String datasetHash) { this.datasetHash = datasetHash; }
    public Integer getCaseCount() { return caseCount; }
    public void setCaseCount(Integer caseCount) { this.caseCount = caseCount; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
    public Integer getSkippedCount() { return skippedCount; }
    public void setSkippedCount(Integer skippedCount) { this.skippedCount = skippedCount; }
    public Integer getTop1Hits() { return top1Hits; }
    public void setTop1Hits(Integer top1Hits) { this.top1Hits = top1Hits; }
    public Integer getTop3Hits() { return top3Hits; }
    public void setTop3Hits(Integer top3Hits) { this.top3Hits = top3Hits; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
