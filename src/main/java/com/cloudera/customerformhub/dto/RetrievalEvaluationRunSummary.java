package com.cloudera.customerformhub.dto;

public record RetrievalEvaluationRunSummary(
        String runId,
        String status,
        String startedAt,
        String completedAt,
        Long durationMs,
        String datasetVersion,
        String datasetHash,
        int evaluationCases,
        int failedCases,
        int skippedCases,
        int top1Hits,
        int top3Hits,
        Double top1HitRate,
        Double top3HitRate,
        String errorMessage
) {}
