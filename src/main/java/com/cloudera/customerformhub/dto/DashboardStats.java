package com.cloudera.customerformhub.dto;

public class DashboardStats {
    private long totalTickets;
    private long closedTickets;
    private long inProgressTickets;
    private long totalQuestions;
    private long answeredFromKnowledgeBase;  // Confirmed answers sourced from the KB
    private long answeredBySme;              // Confirmed answers from SME/manual
    private long overdueSmeRequests;

    // AI coverage as a percentage of all confirmed answers (0-100, one decimal)
    public double getAiCoveragePercent() {
        long totalAnswered = answeredFromKnowledgeBase + answeredBySme;
        if (totalAnswered == 0) return 0.0;
        return Math.round((answeredFromKnowledgeBase * 1000.0) / totalAnswered) / 10.0;
    }

    public long getTotalTickets() { return totalTickets; }
    public void setTotalTickets(long v) { this.totalTickets = v; }

    public long getClosedTickets() { return closedTickets; }
    public void setClosedTickets(long v) { this.closedTickets = v; }

    public long getInProgressTickets() { return inProgressTickets; }
    public void setInProgressTickets(long v) { this.inProgressTickets = v; }

    public long getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(long v) { this.totalQuestions = v; }

    public long getAnsweredFromKnowledgeBase() { return answeredFromKnowledgeBase; }
    public void setAnsweredFromKnowledgeBase(long v) { this.answeredFromKnowledgeBase = v; }

    public long getAnsweredBySme() { return answeredBySme; }
    public void setAnsweredBySme(long v) { this.answeredBySme = v; }

    public long getOverdueSmeRequests() { return overdueSmeRequests; }
    public void setOverdueSmeRequests(long v) { this.overdueSmeRequests = v; }
}