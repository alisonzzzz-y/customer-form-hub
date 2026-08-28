package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.ReviewSummary;
import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiPerformanceServiceTest {
    @Test
    void calculatesMutuallyExclusiveLatestOutcomeRates() {
        FormQuestionRepository repository = mock(FormQuestionRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                reviewed("ACCEPTED", 1), reviewed("EDITED", 2),
                reviewed("ESCALATED", 3), manualAnswer()));

        ReviewSummary summary = new AiPerformanceService(repository).reviewSummary(
                "2026-08-01T00:00:00Z", "2026-09-01T00:00:00Z");

        assertEquals(3, summary.reviewed());
        assertEquals(1, summary.counts().accepted());
        assertEquals(1, summary.counts().edited());
        assertEquals(1, summary.counts().escalated());
        assertEquals(33.3, summary.rates().directAcceptance());
        assertEquals(33.3, summary.rates().humanEdit());
        assertEquals(33.3, summary.rates().rejectedOrEscalated());
    }

    @Test
    void zeroReviewedSuggestionsReturnsNullRates() {
        FormQuestionRepository repository = mock(FormQuestionRepository.class);
        when(repository.findAll()).thenReturn(List.of());
        ReviewSummary summary = new AiPerformanceService(repository).reviewSummary(
                "2026-08-01T00:00:00Z", "2026-09-01T00:00:00Z");
        assertEquals(0, summary.reviewed());
        assertNull(summary.rates().directAcceptance());
        assertNull(summary.rates().humanEdit());
        assertNull(summary.rates().rejectedOrEscalated());
    }

    private FormQuestion reviewed(String outcome, long sourceId) {
        FormQuestion question = new FormQuestion();
        question.setAiSuggestionSourceId(sourceId);
        question.setReviewOutcome(outcome);
        question.setReviewedAt(LocalDateTime.of(2026, 8, 15, 10, 0));
        return question;
    }

    private FormQuestion manualAnswer() {
        FormQuestion question = new FormQuestion();
        question.setReviewedAt(LocalDateTime.of(2026, 8, 15, 10, 0));
        return question;
    }
}
