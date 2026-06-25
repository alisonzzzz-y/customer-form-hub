package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.SearchResult;
import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.repository.KnowledgeBaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final KnowledgeBaseRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RetrievalService(EmbeddingService embeddingService, KnowledgeBaseRepository repository) {
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    // Search for the top 3 most relevant chunks for a question.
    public List<SearchResult> search(String question) {
        // 1. Convert the question into a vector.
        List<Double> questionVector = embeddingService.getEmbedding(question);

        // 2. Get all chunks from the database.
        List<KnowledgeBase> allChunks = repository.findAll();

        // 3. Sort by similarity (high to low), take top 3, and convert to clean SearchResult (no embedding).
        return allChunks.stream()
                .filter(chunk -> Boolean.TRUE.equals(chunk.getApproved()))
                .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty())
                .sorted(Comparator.comparingDouble(
                        (KnowledgeBase chunk) -> cosineSimilarity(questionVector, stringToVector(chunk.getEmbedding()))
                ).reversed())
                .limit(3)
                .map(SearchResult::new)
                .toList();
    }

    // Convert the stored JSON text back into a vector.
    private List<Double> stringToVector(String text) {
        try {
            return objectMapper.readValue(text, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding", e);
        }
    }

    // Calculate cosine similarity between two vectors.
    private double cosineSimilarity(List<Double> a, List<Double> b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}