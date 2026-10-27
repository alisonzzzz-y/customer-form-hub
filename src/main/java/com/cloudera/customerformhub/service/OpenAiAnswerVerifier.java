package com.cloudera.customerformhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Asks gpt-4o-mini whether each retrieved passage answers the question and to
// quote the sentence that does. The quote is checked against the passage by
// AnswerSupportService, so a made-up quote is never shown as evidence.
@Component
public class OpenAiAnswerVerifier implements AnswerVerifier {

    private final RestClient restClient;
    private final boolean enabled;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiAnswerVerifier(RestClient restClient,
                                @Value("${answer-verification.llm.enabled:true}") boolean enabled) {
        this.restClient = restClient;
        this.enabled = enabled;
    }

    @Override
    public List<Verdict> verify(String question, List<String> passages) {
        if (!enabled) throw new IllegalStateException("LLM answer verification is disabled");

        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < passages.size(); i++) {
            numbered.append("[").append(i).append("] ").append(passages.get(i)).append("\n\n");
        }

        String systemPrompt = """
                You check whether approved knowledge passages answer a customer questionnaire question.
                Judge each passage on its own. Do not use outside knowledge.
                DIRECT: the passage itself states the answer to everything that is asked.
                PARTIAL: it answers only part of the question. A question that asks for
                  several things (for example "do you have X, and what is its limit?") is
                  PARTIAL when the passage answers some of them but not all.
                NOT_ANSWERED: it is about a related topic but does not answer the question
                  (for example certifications when the question asks for a DPA).
                FUTURE_ONLY: it only describes audits, renewals, plans or upcoming events,
                  while the question asks about the current state.
                For DIRECT and PARTIAL, copy the single sentence that answers it, word for word.
                Respond with ONLY a JSON array, no other text, in this exact format:
                [{"index": 0, "verdict": "DIRECT", "evidence": "..."}]
                """;

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content",
                                "Question: " + question + "\n\nPassages:\n" + numbered)
                ),
                "temperature", 0
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return parse(extractContent(response), passages.size());
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    // A passage the model skipped stays null (not verified); an unreadable reply
    // throws, so the caller falls back to the rule-based check.
    List<Verdict> parse(String content, int count) {
        List<Verdict> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(null);
        JsonNode array;
        try {
            String cleaned = content.trim()
                    .replaceAll("^```json", "")
                    .replaceAll("^```", "")
                    .replaceAll("```$", "")
                    .trim();
            array = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new IllegalStateException("Unreadable verification reply", e);
        }
        for (JsonNode item : array) {
            int index = item.path("index").asInt(-1);
            String verdict = item.path("verdict").asText("");
            if (index < 0 || index >= count) continue;
            if (!List.of("DIRECT", "PARTIAL", "NOT_ANSWERED", "FUTURE_ONLY").contains(verdict)) continue;
            result.set(index, new Verdict(verdict, item.path("evidence").asText("")));
        }
        return result;
    }
}
