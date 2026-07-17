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
    private static final double SIMILARITY_THRESHOLD = 0.35;

    public RetrievalService(EmbeddingService embeddingService, KnowledgeBaseRepository repository) {
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    // A small helper that pairs a chunk with its similarity score.
    private record ScoredChunk(KnowledgeBase chunk, double score) {
    }

    // Search for the top 3 most relevant chunks for a question.
    public List<SearchResult> search(String question) {
        // 1. Convert the question into a vector.
        List<Double> questionVector = embeddingService.getEmbedding(question);

        // 2. Get all chunks from the database.
        List<KnowledgeBase> allChunks = repository.findAll();

        // 3. For each valid chunk, compute its similarity ONCE and keep it with the chunk.
        return allChunks.stream()
                // Only entries in the real Approved lifecycle state may feed AI.
                // The approved flag is retained as a compatibility guard.
                .filter(chunk -> "Approved".equals(chunk.getStatus())
                        && Boolean.TRUE.equals(chunk.getApproved()))
                .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty())
                .map(chunk -> new ScoredChunk(
                        chunk,
                        cosineSimilarity(questionVector, stringToVector(chunk.getEmbedding()))
                ))
                // Drop chunks below the similarity threshold (irrelevant noise)
                .filter(scored -> scored.score() >= SIMILARITY_THRESHOLD)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(3)
                .map(scored -> {
                    SearchResult result = new SearchResult(scored.chunk());
                    result.setSimilarityScore(scored.score());
                    return result;
                })
                .toList();
    }

    // Convert the stored JSON text back into a vector.
    private List<Double> stringToVector(String text) {
        try {
            return objectMapper.readValue(text, new TypeReference<List<Double>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding", e);
        }
    }

    // Calculate cosine similarity between two vectors.
    double cosineSimilarity(List<Double> a, List<Double> b) {
        // Guard: null or length mismatch means we can't compare → treat as no similarity
        if (a == null || b == null || a.size() != b.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        // Guard: a zero-length vector would divide by zero → return 0 instead of NaN
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }
}
