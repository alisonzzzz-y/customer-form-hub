package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.ReviewSummary;
import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class AiPerformanceService {

    private final FormQuestionRepository questionRepository;

    public AiPerformanceService(FormQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public ReviewSummary reviewSummary(String fromValue, String toValue) {
        Instant to = parseInstant(toValue, Instant.now());
        Instant from = parseInstant(fromValue, to.minusSeconds(30L * 24 * 60 * 60));
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be earlier than to");
        }

        LocalDateTime fromUtc = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
        LocalDateTime toUtc = LocalDateTime.ofInstant(to, ZoneOffset.UTC);
        List<FormQuestion> reviewedQuestions = questionRepository.findAll().stream()
                .filter(q -> q.getAiSuggestionSourceId() != null)
                .filter(q -> q.getReviewOutcome() != null)
                .filter(q -> q.getReviewedAt() != null)
                .filter(q -> !q.getReviewedAt().isBefore(fromUtc) && q.getReviewedAt().isBefore(toUtc))
                .toList();

        long accepted = count(reviewedQuestions, "ACCEPTED");
        long edited = count(reviewedQuestions, "EDITED");
        long escalated = count(reviewedQuestions, "ESCALATED");
        long rejected = 0;
        long reviewed = accepted + edited + rejected + escalated;

        ReviewSummary.Rates rates = reviewed == 0
                ? new ReviewSummary.Rates(null, null, null)
                : new ReviewSummary.Rates(
                        percent(accepted, reviewed),
                        percent(edited, reviewed),
                        percent(rejected + escalated, reviewed));

        return new ReviewSummary(
                new ReviewSummary.Period(from.toString(), to.toString(), "UTC"),
                reviewed,
                new ReviewSummary.Counts(accepted, edited, rejected, escalated),
                rates);
    }

    private long count(List<FormQuestion> questions, String outcome) {
        return questions.stream().filter(q -> outcome.equals(q.getReviewOutcome())).count();
    }

    private double percent(long value, long total) {
        return Math.round(value * 1000.0 / total) / 10.0;
    }

    private Instant parseInstant(String value, Instant fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Dates must be ISO-8601 UTC timestamps");
        }
    }
}
