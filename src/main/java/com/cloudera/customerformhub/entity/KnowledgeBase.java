package com.cloudera.customerformhub.entity;

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
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String answer;

    @Column(length = 500)
    private String source;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "sharing_status", length = 50)
    private String sharingStatus;

    @Column(length = 100)
    private String department;

    @Column(columnDefinition = "LONGTEXT")
    private String embedding;

    @Column(nullable = false)
    private Boolean approved = false;

    //constructors
    public KnowledgeBase() {
    }

    public KnowledgeBase(
            String question,
            String answer,
            String source,
            LocalDateTime lastUpdated,
            String sharingStatus,
            String department,
            String embedding,
            Boolean approved
    ) {
        this.question = question;
        this.answer = answer;
        this.source = source;
        this.lastUpdated = lastUpdated;
        this.sharingStatus = sharingStatus;
        this.department = department;
        this.embedding = embedding;
        this.approved = approved;
    }

    //set default values automatically before a new record is saved
    @PrePersist
    public void beforeInsert() {
        if (this.approved == null) {
            this.approved = false;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

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
}
