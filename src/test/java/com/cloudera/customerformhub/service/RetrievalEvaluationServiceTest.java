package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.RetrievalEvaluationRunSummary;
import com.cloudera.customerformhub.dto.SearchResult;
import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.repository.RetrievalEvaluationRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RetrievalEvaluationServiceTest {
    @Test
    void shortResultListsAreMissesAndTechnicalFailuresAreSeparate() {
        RetrievalService retrieval = mock(RetrievalService.class);
        RetrievalEvaluationRunRepository repository = mock(RetrievalEvaluationRunRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(retrieval.search(any())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0);
            if (query.contains("payment terms")) throw new RuntimeException("embedding unavailable");
            if (query.contains("encrypted at rest")) {
                KnowledgeBase source = new KnowledgeBase();
                source.setSourceKey("information-security-policy-v3-2-encryption-of-data-at-rest");
                return List.of(new SearchResult(source));
            }
            return List.of();
        });

        RetrievalEvaluationRunSummary result = new RetrievalEvaluationService(
                retrieval, repository, new ObjectMapper()).runEvaluation();

        assertEquals("COMPLETED", result.status());
        assertEquals(11, result.evaluationCases());
        assertEquals(1, result.failedCases());
        assertEquals(1, result.top1Hits());
        assertEquals(1, result.top3Hits());
        assertEquals(9.1, result.top1HitRate());
        assertEquals(9.1, result.top3HitRate());
    }
}
