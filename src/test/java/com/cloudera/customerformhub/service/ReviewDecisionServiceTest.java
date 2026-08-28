package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewDecisionServiceTest {
    @Test
    void aeRequestEndsCurrentReviewAndKeepsOperationalTimestamp() {
        FormQuestionRepository repository = mock(FormQuestionRepository.class);
        FormQuestion question = new FormQuestion();
        when(repository.findById(7L)).thenReturn(Optional.of(question));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FormQuestion result = new ReviewDecisionService(repository).escalate(7L, "AE", 42L);

        assertEquals("Waiting AE", result.getStatus());
        assertEquals("ESCALATED", result.getReviewOutcome());
        assertEquals(42L, result.getAiSuggestionSourceId());
        assertNotNull(result.getReviewedAt());
        assertNotNull(result.getAeClarificationRequestedAt());
    }

    @Test
    void manualQuestionCanWaitForSmeWithoutEnteringAiMetrics() {
        FormQuestionRepository repository = mock(FormQuestionRepository.class);
        FormQuestion question = new FormQuestion();
        when(repository.findById(8L)).thenReturn(Optional.of(question));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FormQuestion result = new ReviewDecisionService(repository).escalate(8L, "SME", null);

        assertEquals("SME Needed", result.getStatus());
        assertNull(result.getReviewOutcome());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    void reopenClearsCurrentOutcomeButPreservesAeRequestTime() {
        FormQuestionRepository repository = mock(FormQuestionRepository.class);
        FormQuestion question = new FormQuestion();
        question.setReviewOutcome("ESCALATED");
        question.setReviewedAt(java.time.LocalDateTime.now());
        question.setAeClarificationRequestedAt(java.time.LocalDateTime.now());
        when(repository.findById(9L)).thenReturn(Optional.of(question));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FormQuestion result = new ReviewDecisionService(repository).reopen(9L);

        assertEquals("Needs Review", result.getStatus());
        assertNull(result.getReviewOutcome());
        assertNull(result.getReviewedAt());
        assertNotNull(result.getAeClarificationRequestedAt());
    }
}
