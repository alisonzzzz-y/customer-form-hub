package com.cloudera.customerformhub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_base")
public class KnowledgeBase {
    //PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//Other attributes
    //from which document
    @Column(name = "document_title", length = 255)
    private String documentTitle;

    //from which section of the document
    @Column(name = "section_title", length = 255)
    private String sectionTitle;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 500)
    private String source;

    // Stable benchmark identifier. Unlike the generated database ID, this is
    // consistent across environments and dataset reloads.
    @Column(name = "source_key", length = 255)
    private String sourceKey;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "sharing_status", length = 50)
    private String sharingStatus;

    @Column(length = 100)
    private String department;

    @JsonIgnore
    @Column(columnDefinition = "LONGTEXT")
    private String embedding;

    @Column(nullable = false)
    private Boolean approved = false;

    // Full knowledge lifecycle from the PRD. Kept alongside approved for
    // backwards compatibility with older clients and existing database rows.
    @Column(length = 50)
    private String status;

    //constructors
    public KnowledgeBase() {
    }

    public KnowledgeBase(
            String documentTitle,
            String sectionTitle,
            String content,
            String source,
            LocalDateTime lastUpdated,
            String sharingStatus,
            String department,
            String embedding,
            Boolean approved
    ) {
        this.documentTitle = documentTitle;
        this.sectionTitle = sectionTitle;
        this.content = content;
        this.source = source;
        this.lastUpdated = lastUpdated;
        this.sharingStatus = sharingStatus;
        this.department = department;
        this.embedding = embedding;
        this.approved = approved;
        this.status = Boolean.TRUE.equals(approved) ? "Approved" : "Pending Review";
    }

    // Keep the legacy approved flag and the richer lifecycle status aligned.
    @PrePersist
    public void beforeInsert() {
        synchroniseLifecycleState();
    }

    @PreUpdate
    public void beforeUpdate() {
        synchroniseLifecycleState();
    }

    public void synchroniseLifecycleState() {
        if (this.status == null || this.status.isBlank()) {
            this.status = Boolean.TRUE.equals(this.approved) ? "Approved" : "Pending Review";
        }
        this.approved = "Approved".equals(this.status);
        if (this.sourceKey == null || this.sourceKey.isBlank()) {
            this.sourceKey = buildSourceKey(this.documentTitle, this.sectionTitle);
        }
    }

    public static String buildSourceKey(String documentTitle, String sectionTitle) {
        String raw = ((documentTitle == null ? "source" : documentTitle) + "-"
                + (sectionTitle == null ? "entry" : sectionTitle)).toLowerCase();
        String key = raw.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return key.isBlank() ? "source-entry" : key;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getSharingStatus() {
        return sharingStatus;
    }

    public void setSharingStatus(String sharingStatus) {
        this.sharingStatus = sharingStatus;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
