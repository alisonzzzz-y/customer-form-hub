package com.cloudera.customerformhub.dto;

public record ReviewSummary(
        Period period,
        long reviewed,
        Counts counts,
        Rates rates
) {
    public record Period(String from, String to, String timezone) {}
    public record Counts(long accepted, long edited, long rejected, long escalated) {}
    public record Rates(Double directAcceptance, Double humanEdit, Double rejectedOrEscalated) {}
}
