package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.repository.KnowledgeBaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final RestClient restClient;
    private final KnowledgeBaseRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();  // Used to convert vectors to and from JSON.

    @Value("${openai.api.key}")
    private String apiKey;

    public EmbeddingService(RestClient restClient, KnowledgeBaseRepository repository) {
        this.restClient = restClient;
        this.repository = repository;
    }

    // Convert text into an embedding vector.
    public List<Double> getEmbedding(String text) {
        Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-3-small",
                "input", text
        );

        Map<String, Object> response = restClient.post()
                .uri("https://api.openai.com/v1/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<Double> embedding = (List<Double>) data.get(0).get("embedding");
        return embedding;
    }

    // Convert a vector to a JSON string for database storage.
    public String vectorToString(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert vector to string", e);
        }
    }

    // Generate and save embeddings for chunks without one.
    public void generateEmbeddingsForAll() {
        List<KnowledgeBase> all = repository.findAll();
        int count = 0;

        for (KnowledgeBase chunk : all) {
            // Skip chunks that already have an embedding.
            if (chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty()) {
                continue;
            }

            // 1. Send the chunk text to OpenAI and get the embedding.
            List<Double> vector = getEmbedding(chunk.getContent());

            // 2. Convert it to JSON and store it in the embedding field.
            chunk.setEmbedding(vectorToString(vector));

            // 3. Save the chunk to the database.
            repository.save(chunk);
            count++;
            System.out.println(">>> Generated embedding for chunk id " + chunk.getId());
        }

        System.out.println(">>> Done. Generated embeddings for " + count + " chunks.");
    }
}