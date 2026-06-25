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
    private final ObjectMapper objectMapper = new ObjectMapper();  // 用来打包/解包向量

    @Value("${openai.api.key}")
    private String apiKey;

    public EmbeddingService(RestClient restClient, KnowledgeBaseRepository repository) {
        this.restClient = restClient;
        this.repository = repository;
    }

    // 输入一段文字,返回它的向量(一串数字)
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

    // 把一串数字向量"打包"成一段文字(JSON 字符串),方便存进数据库
    public String vectorToString(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert vector to string", e);
        }
    }

    // 给所有还没有向量的 chunk 生成向量并存进数据库
    public void generateEmbeddingsForAll() {
        List<KnowledgeBase> all = repository.findAll();
        int count = 0;

        for (KnowledgeBase chunk : all) {
            // 如果这条已经有向量了,跳过(避免重复花钱)
            if (chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty()) {
                continue;
            }

            // 1. 把这条的文字发给 OpenAI,拿回向量
            List<Double> vector = getEmbedding(chunk.getContent());

            // 2. 打包成文字,存进 embedding 字段
            chunk.setEmbedding(vectorToString(vector));

            // 3. 存回数据库
            repository.save(chunk);
            count++;
            System.out.println(">>> Generated embedding for chunk id " + chunk.getId());
        }

        System.out.println(">>> Done. Generated embeddings for " + count + " chunks.");
    }
}