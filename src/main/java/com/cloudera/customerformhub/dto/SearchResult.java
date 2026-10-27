package com.cloudera.customerformhub.dto;

import com.cloudera.customerformhub.entity.KnowledgeBase;

import java.time.LocalDateTime;

// A clean response object for search results (no embedding)
public class SearchResult {

    private Long id;
    private String documentTitle;
    private String sectionTitle;
    private String content;
    private String source;
    private String sourceKey;
    private LocalDateTime lastUpdated;
    private String sharingStatus;
    private String department;
    private Boolean approved;
    private Double similarityScore; // cosine similarity to the question (0 to 1)
    // How close the passage is to the question, from the score alone:
    // HIGH / MEDIUM / LOW. Relevance is not evidence.
    private String relevanceLevel;
    // Whether the passage supports an answer (AnswerSupportService):
    // SUPPORTED / PARTIAL / RELATED. Every level still needs human review.
    private String supportLevel;
    private String evidence;       // sentence from the passage that answers the question
    private String supportReason;  // why this support level was given

    // Build a SearchResult from a KnowledgeBase entity (embedding is left out)
    public SearchResult(KnowledgeBase chunk) {
        this.id = chunk.getId();
        this.documentTitle = chunk.getDocumentTitle();
        this.sectionTitle = chunk.getSectionTitle();
        this.content = chunk.getContent();
        this.source = chunk.getSource();
        this.sourceKey = chunk.getSourceKey();
        this.lastUpdated = chunk.getLastUpdated();
        this.sharingStatus = chunk.getSharingStatus();
        this.department = chunk.getDepartment();
        this.approved = chunk.getApproved();
    }

    // Getters (needed so the JSON can be built)
    public Long getId() {
        return id;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }

    public String getSourceKey() { return sourceKey; }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getSharingStatus() {
        return sharingStatus;
    }

    public String getDepartment() {
        return department;
    }

    public Boolean getApproved() {
        return approved;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    // Setter so the retrieval service can fill in the score after construction
    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getSupportLevel() {
        return supportLevel;
    }

    public void setSupportLevel(String supportLevel) {
        this.supportLevel = supportLevel;
    }

    public String getRelevanceLevel() {
        return relevanceLevel;
    }

    public void setRelevanceLevel(String relevanceLevel) {
        this.relevanceLevel = relevanceLevel;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getSupportReason() {
        return supportReason;
    }

    public void setSupportReason(String supportReason) {
        this.supportReason = supportReason;
    }
}
