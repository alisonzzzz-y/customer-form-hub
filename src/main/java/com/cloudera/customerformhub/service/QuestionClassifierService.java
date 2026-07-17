package com.cloudera.customerformhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.cloudera.customerformhub.service.QuestionnaireParserService.ParsedQuestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QuestionClassifierService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // The fixed set of departments the LLM must choose from
    private static final List<String> DEPARTMENTS = List.of(
            "InfoSec", "Legal", "HR", "Finance", "Compliance", "ESG", "General");

    public QuestionClassifierService(RestClient restClient) {
        this.restClient = restClient;
    }

    // Classify a list of questions into departments in one LLM call.
    // Returns a list of departments, aligned by index with the input questions.
    public List<String> classify(List<String> questions) {
        if (questions == null || questions.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Build the prompt: number each question, ask for JSON back
        StringBuilder questionList = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            questionList.append(i).append(". ").append(questions.get(i)).append("\n");
        }

        String systemPrompt = """
                You are a classifier that routes supplier security questionnaire questions
                to the correct internal department. The available departments are:
                InfoSec, Legal, HR, Finance, Compliance, ESG, General.
                Choose exactly one department for each question.
                Use "General" only if none of the others clearly fit.
                Respond with ONLY a JSON array, no other text, in this exact format:
                [{"index": 0, "department": "InfoSec"}, {"index": 1, "department": "Legal"}]
                """;

        String userPrompt = "Classify these questions:\n" + questionList;

        // 2. Build the request body for the chat completions API
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0
        );

        // 3. Call OpenAI
        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        // 4. Extract the model's text answer
        String content = extractContent(response);

        // 5. Parse the JSON array the model returned, map index -> department
        return parseDepartments(content, questions.size());
    }

    // Extract questionnaire questions from raw document text in one LLM call.
    public List<ParsedQuestion> extractQuestions(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new ArrayList<>();
        }

        String boundedText = rawText;
        if (boundedText.length() > 15000) {
            boundedText = boundedText.substring(0, 15000);
        }

        String systemPrompt = """
                You extract questions from supplier/security questionnaire documents.
                Given raw document text, identify every question or information request a supplier is expected to answer.
                Ignore headings, instructions, boilerplate.
                Respond with ONLY a JSON array, no other text, in this exact format:
                [{"section": "...", "question": "..."}]
                Use an empty string for section if none is identifiable.
                Preserve the original question wording.
                """;

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", boundedText)
                ),
                "temperature", 0
        );

        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        String content = extractContent(response);
        return parseExtractedQuestions(content);
    }

    // Pull the assistant's message text out of the OpenAI response
    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    // Parse the JSON the model returned into a department list aligned by index.
    // Falls back to "General" for anything missing or invalid.
    private List<String> parseDepartments(String content, int count) {
        // default everything to "General", then fill in what the model gave us
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add("General");
        }

        try {
            // The model may wrap JSON in ```json ... ```, strip that if present
            String cleaned = content.trim()
                    .replaceAll("^```json", "")
                    .replaceAll("^```", "")
                    .replaceAll("```$", "")
                    .trim();

            JsonNode array = objectMapper.readTree(cleaned);
            for (JsonNode node : array) {
                int index = node.get("index").asInt();
                String dept = node.get("department").asText();
                if (index >= 0 && index < count && DEPARTMENTS.contains(dept)) {
                    result.set(index, dept);
                }
            }
        } catch (Exception e) {
            // If parsing fails, everything stays "General" (safe fallback)
            System.err.println("Failed to parse classification JSON: " + e.getMessage());
        }

        return result;
    }

    // Parse extracted question JSON into ParsedQuestion records.
    private List<ParsedQuestion> parseExtractedQuestions(String content) {
        List<ParsedQuestion> result = new ArrayList<>();

        try {
            String cleaned = content.trim()
                    .replaceAll("^```json", "")
                    .replaceAll("^```", "")
                    .replaceAll("```$", "")
                    .trim();

            JsonNode array = objectMapper.readTree(cleaned);
            for (JsonNode node : array) {
                JsonNode questionNode = node.get("question");
                if (questionNode == null || questionNode.asText().isBlank()) {
                    continue;
                }

                JsonNode sectionNode = node.get("section");
                String section = sectionNode == null ? "" : sectionNode.asText();
                String question = questionNode.asText();
                result.add(new ParsedQuestion(section, question));
            }
        } catch (Exception e) {
            System.err.println("Failed to parse extracted questions JSON: " + e.getMessage());
            return new ArrayList<>();
        }

        return result;
    }
}
