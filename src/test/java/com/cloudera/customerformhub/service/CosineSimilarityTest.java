package com.cloudera.customerformhub.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosineSimilarityTest {

    // We only need to test the cosineSimilarity method, which doesn't use
    // the embedding service or repository, so we can pass null for those.
    private final RetrievalService service = new RetrievalService(null, null);

    @Test
    void identicalVectors_returnOne() {
        List<Double> a = List.of(1.0, 2.0, 3.0);
        List<Double> b = List.of(1.0, 2.0, 3.0);
        // Identical direction → cosine similarity is exactly 1
        assertEquals(1.0, service.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void orthogonalVectors_returnZero() {
        List<Double> a = List.of(1.0, 0.0);
        List<Double> b = List.of(0.0, 1.0);
        // Perpendicular vectors → cosine similarity is 0
        assertEquals(0.0, service.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void oppositeVectors_returnMinusOne() {
        List<Double> a = List.of(1.0, 0.0);
        List<Double> b = List.of(-1.0, 0.0);
        // Opposite direction → cosine similarity is -1
        assertEquals(-1.0, service.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void differentLengths_returnZero() {
        List<Double> a = List.of(1.0, 2.0, 3.0);
        List<Double> b = List.of(1.0, 2.0);
        // Length mismatch → guard returns 0
        assertEquals(0.0, service.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void zeroVector_returnZero() {
        List<Double> a = List.of(0.0, 0.0, 0.0);
        List<Double> b = List.of(1.0, 2.0, 3.0);
        // Zero vector → denominator is 0 → guard returns 0 (not NaN)
        assertEquals(0.0, service.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void nullVector_returnZero() {
        List<Double> b = List.of(1.0, 2.0, 3.0);
        // Null input → guard returns 0
        assertEquals(0.0, service.cosineSimilarity(null, b), 0.0001);
    }
}