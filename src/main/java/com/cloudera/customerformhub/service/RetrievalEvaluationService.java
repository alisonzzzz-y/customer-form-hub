package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.RetrievalEvaluationRunSummary;
import com.cloudera.customerformhub.dto.SearchResult;
import com.cloudera.customerformhub.entity.RetrievalEvaluationRun;
import com.cloudera.customerformhub.repository.RetrievalEvaluationRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RetrievalEvaluationService {
    private static final String DATASET_PATH = "ai-performance/retrieval-benchmark-v1.json";

    private final RetrievalService retrievalService;
    private final RetrievalEvaluationRunRepository runRepository;
    private final ObjectMapper objectMapper;

    public RetrievalEvaluationService(RetrievalService retrievalService,
                                      RetrievalEvaluationRunRepository runRepository,
                                      ObjectMapper objectMapper) {
        this.retrievalService = retrievalService;
        this.runRepository = runRepository;
        this.objectMapper = objectMapper;
    }

    public RetrievalEvaluationRunSummary runEvaluation() {
        long startedNanos = System.nanoTime();
        RetrievalEvaluationRun run = new RetrievalEvaluationRun();
        run.setId(UUID.randomUUID().toString());
        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        run.setCaseCount(0);
        run.setFailedCount(0);
        run.setSkippedCount(0);
        run.setTop1Hits(0);
        run.setTop3Hits(0);
        runRepository.save(run);

        try {
            byte[] bytes = new ClassPathResource(DATASET_PATH).getInputStream().readAllBytes();
            BenchmarkDataset dataset = objectMapper.readValue(bytes, BenchmarkDataset.class);
            validate(dataset);
            run.setDatasetVersion(dataset.version());
            run.setDatasetHash(sha256(bytes));

            int valid = 0;
            int failed = 0;
            int skipped = 0;
            int top1 = 0;
            int top3 = 0;
            for (BenchmarkCase benchmarkCase : dataset.cases()) {
                if (!benchmarkCase.active()) {
                    skipped++;
                    continue;
                }
                try {
                    List<SearchResult> results = retrievalService.search(benchmarkCase.query());
                    List<String> retrieved = results.stream()
                            .map(SearchResult::getSourceKey)
                            .filter(key -> key != null && !key.isBlank())
                            .toList();
                    Set<String> expected = Set.copyOf(benchmarkCase.expectedSourceIds());
                    valid++;
                    if (!retrieved.isEmpty() && expected.contains(retrieved.get(0))) top1++;
                    if (retrieved.stream().limit(3).anyMatch(expected::contains)) top3++;
                } catch (RuntimeException ex) {
                    failed++;
                }
            }

            run.setStatus("COMPLETED");
            run.setCaseCount(valid);
            run.setFailedCount(failed);
            run.setSkippedCount(skipped);
            run.setTop1Hits(top1);
            run.setTop3Hits(top3);
        } catch (Exception ex) {
            run.setStatus("FAILED");
            run.setErrorMessage(safeMessage(ex));
        }

        run.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        run.setDurationMs((System.nanoTime() - startedNanos) / 1_000_000);
        return toSummary(runRepository.save(run));
    }

    public List<RetrievalEvaluationRunSummary> list(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        return runRepository.findAll(Sort.by(Sort.Direction.DESC, "completedAt")).stream()
                .limit(limit)
                .map(this::toSummary)
                .toList();
    }

    public RetrievalEvaluationRunSummary get(String runId) {
        return runRepository.findById(runId).map(this::toSummary).orElse(null);
    }

    private RetrievalEvaluationRunSummary toSummary(RetrievalEvaluationRun run) {
        int cases = value(run.getCaseCount());
        return new RetrievalEvaluationRunSummary(
                run.getId(), run.getStatus(), iso(run.getStartedAt()), iso(run.getCompletedAt()),
                run.getDurationMs(), run.getDatasetVersion(), run.getDatasetHash(), cases,
                value(run.getFailedCount()), value(run.getSkippedCount()), value(run.getTop1Hits()),
                value(run.getTop3Hits()), rate(run.getTop1Hits(), cases), rate(run.getTop3Hits(), cases),
                run.getErrorMessage());
    }

    private void validate(BenchmarkDataset dataset) {
        if (dataset == null || dataset.version() == null || dataset.version().isBlank()
                || dataset.cases() == null || dataset.cases().isEmpty()) {
            throw new IllegalArgumentException("Benchmark dataset requires a version and cases");
        }
        Set<String> ids = dataset.cases().stream().map(BenchmarkCase::id).collect(Collectors.toSet());
        if (ids.size() != dataset.cases().size()) throw new IllegalArgumentException("Benchmark case IDs must be unique");
        for (BenchmarkCase c : dataset.cases()) {
            if (c.id() == null || c.id().isBlank() || c.query() == null || c.query().isBlank()
                    || c.expectedSourceIds() == null || c.expectedSourceIds().isEmpty()) {
                throw new IllegalArgumentException("Every benchmark case requires id, query and expectedSourceIds");
            }
        }
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private String safeMessage(Exception ex) {
        String value = ex.getMessage();
        if (value == null || value.isBlank()) return "Evaluation failed";
        return value.substring(0, Math.min(value.length(), 500));
    }

    private int value(Integer number) { return number == null ? 0 : number; }
    private Double rate(Integer hits, int cases) {
        return cases == 0 ? null : Math.round(value(hits) * 1000.0 / cases) / 10.0;
    }
    private String iso(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC).toString();
    }

    public record BenchmarkDataset(String version, List<BenchmarkCase> cases) {}
    public record BenchmarkCase(String id, String query, List<String> expectedSourceIds, boolean active) {}
}
