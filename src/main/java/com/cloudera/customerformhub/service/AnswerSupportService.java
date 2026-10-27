package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Decides whether a retrieved passage supports an answer, separately from how
// similar it is. Similarity only says the passage is about a related topic.
//
//   SUPPORTED  the rule check passes and the verifier found a sentence in the
//              passage that states the answer (quoted as evidence)
//   PARTIAL    worth offering as a draft, but must be checked: the verifier
//              said it answers part of the question, or it could not run and
//              only the rule check passed
//   RELATED    shown as related knowledge only, never as an answer
@Service
public class AnswerSupportService {

    public static final String SUPPORTED = "SUPPORTED";
    public static final String PARTIAL = "PARTIAL";
    public static final String RELATED = "RELATED";

    // Below this similarity a passage is shown as related knowledge only.
    static final double MIN_ANSWER_SCORE = 0.50;

    private static final Logger log = LoggerFactory.getLogger(AnswerSupportService.class);

    private final AnswerVerifier verifier;

    public AnswerSupportService(AnswerVerifier verifier) {
        this.verifier = verifier;
    }

    public List<SearchResult> assess(String question, List<SearchResult> results) {
        List<EvidenceRules.Finding> findings = new ArrayList<>();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            EvidenceRules.Finding f = EvidenceRules.check(question, r.getContent());
            findings.add(f);
            if (f.candidate() && score(r) >= MIN_ANSWER_SCORE) candidates.add(i);
        }

        // Only passages the rules did not rule out are sent to the verifier.
        List<AnswerVerifier.Verdict> verdicts = null;
        if (!candidates.isEmpty()) {
            try {
                verdicts = verifier.verify(question,
                        candidates.stream().map(i -> results.get(i).getContent()).toList());
            } catch (RuntimeException e) {
                log.warn("Answer verification unavailable, using rule check only: {}", e.getMessage());
            }
        }

        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            EvidenceRules.Finding f = findings.get(i);
            if (score(r) < MIN_ANSWER_SCORE) {
                set(r, RELATED, "", "Similarity is too low for this to be used as an answer.");
            } else if (!f.candidate()) {
                set(r, RELATED, "", f.reason());
            } else {
                int at = candidates.indexOf(i);
                AnswerVerifier.Verdict v = verdicts == null || at >= verdicts.size() ? null : verdicts.get(at);
                decide(r, f, v);
            }
        }
        return results;
    }

    private void decide(SearchResult r, EvidenceRules.Finding f, AnswerVerifier.Verdict v) {
        if (v == null) {
            set(r, PARTIAL, f.sentence(),
                    "The source names what was asked, but the answer was not independently verified. Check before approving.");
            return;
        }
        boolean quoteFound = v.evidence() != null && !v.evidence().isBlank()
                && contains(r.getContent(), v.evidence());
        switch (v.verdict()) {
            case "DIRECT" -> {
                if (quoteFound) set(r, SUPPORTED, v.evidence().trim(), "The source states the answer directly.");
                else set(r, PARTIAL, f.sentence(),
                        "The verifier's quote is not in the source. Check before approving.");
            }
            case "PARTIAL" -> set(r, PARTIAL, quoteFound ? v.evidence().trim() : f.sentence(),
                    "The source answers only part of the question. Check before approving.");
            case "FUTURE_ONLY" -> set(r, RELATED, "",
                    "The source only describes audits, renewals or plans, not the current status asked about.");
            default -> set(r, RELATED, "", "The source is about a related topic but does not answer the question.");
        }
    }

    private static void set(SearchResult r, String level, String evidence, String reason) {
        r.setSupportLevel(level);
        r.setEvidence(evidence);
        r.setSupportReason(reason);
    }

    private static double score(SearchResult r) {
        return r.getSimilarityScore() == null ? 0 : r.getSimilarityScore();
    }

    // Whitespace- and case-insensitive containment, so a quote with different
    // line breaks still counts, but a paraphrase does not.
    static boolean contains(String passage, String quote) {
        String p = passage.toLowerCase().replaceAll("\\s+", " ");
        String q = quote.toLowerCase().replaceAll("\\s+", " ").trim();
        if (q.endsWith(".")) q = q.substring(0, q.length() - 1);
        return !q.isEmpty() && p.contains(q);
    }
}
