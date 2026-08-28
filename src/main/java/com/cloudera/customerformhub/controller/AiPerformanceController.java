package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.dto.ReviewSummary;
import com.cloudera.customerformhub.service.AiPerformanceService;
import com.cloudera.customerformhub.dto.RetrievalEvaluationRunSummary;
import com.cloudera.customerformhub.service.RetrievalEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-performance")
public class AiPerformanceController {

    private final AiPerformanceService aiPerformanceService;
    private final RetrievalEvaluationService retrievalEvaluationService;

    public AiPerformanceController(AiPerformanceService aiPerformanceService,
                                   RetrievalEvaluationService retrievalEvaluationService) {
        this.aiPerformanceService = aiPerformanceService;
        this.retrievalEvaluationService = retrievalEvaluationService;
    }

    @GetMapping("/review-summary")
    public ReviewSummary reviewSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return aiPerformanceService.reviewSummary(from, to);
    }

    @GetMapping("/retrieval-runs")
    public java.util.List<RetrievalEvaluationRunSummary> retrievalRuns(
            @RequestParam(defaultValue = "10") int limit) {
        return retrievalEvaluationService.list(limit);
    }

    @GetMapping("/retrieval-runs/{runId}")
    public ResponseEntity<RetrievalEvaluationRunSummary> retrievalRun(
            @org.springframework.web.bind.annotation.PathVariable String runId) {
        RetrievalEvaluationRunSummary run = retrievalEvaluationService.get(runId);
        return run == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(run);
    }
}
