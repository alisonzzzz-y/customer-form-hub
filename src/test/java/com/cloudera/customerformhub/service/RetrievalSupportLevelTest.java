package com.cloudera.customerformhub.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetrievalSupportLevelTest {

    @Test
    void scoresAboveTheSupportedThresholdCanBeOfferedAsADraftAnswer() {
        assertEquals("SUPPORTED", RetrievalService.supportLevel(0.60));
        assertEquals("SUPPORTED", RetrievalService.supportLevel(0.82));
    }

    @Test
    void midRangeScoresArePartialMatches() {
        assertEquals("PARTIAL", RetrievalService.supportLevel(0.50));
        assertEquals("PARTIAL", RetrievalService.supportLevel(0.59));
    }

    @Test
    void weakMatchesJustAboveTheSearchCutoffAreOnlyRelatedKnowledge() {
        // e.g. the DPA question that matched a certifications summary at 36%
        assertEquals("RELATED", RetrievalService.supportLevel(0.36));
        assertEquals("RELATED", RetrievalService.supportLevel(0.4999));
    }
}
