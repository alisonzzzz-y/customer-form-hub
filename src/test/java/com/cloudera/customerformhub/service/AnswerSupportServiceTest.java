package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.SearchResult;
import com.cloudera.customerformhub.entity.KnowledgeBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// Similarity says a passage is about a related topic; these tests pin down that
// a high score alone never makes a passage count as an answer.
class AnswerSupportServiceTest {

    // Same text as the seeded knowledge base entries.
    static final String CERTIFICATIONS_HELD =
            "Cloudera maintains SOC 2 Type II and ISO/IEC 27001 certification for its data platform, and operates "
                    + "in accordance with the EU General Data Protection Regulation (GDPR) and the California Consumer "
                    + "Privacy Act (CCPA). Cloudera also maintains a FedRAMP Moderate authorization for its government "
                    + "offering and supports PCI DSS.";
    static final String UPCOMING_AUDITS =
            "The ISO 27001 surveillance audit and the annual SOC 2 Type II examination are conducted on a recurring "
                    + "annual cycle. The next surveillance activities are scheduled within the next twelve months.";
    static final String DPA_TEMPLATE =
            "Cloudera offers a standard Data Processing Addendum (DPA) to customers. The DPA incorporates the EU "
                    + "Standard Contractual Clauses.";

    static final String DPA_QUESTION = "Is a data processing agreement available?";
    static final String ISO_QUESTION = "Do you currently hold ISO/IEC 27001 certification?";

    private static SearchResult result(String content, double score) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setContent(content);
        SearchResult r = new SearchResult(kb);
        r.setSimilarityScore(score);
        r.setRelevanceLevel(RetrievalService.relevanceLevel(score));
        return r;
    }

    // A verifier that says DIRECT for everything, quoting the first sentence:
    // the rule check must still stop passages that do not answer.
    private static final AnswerVerifier AGREES_WITH_EVERYTHING = (q, passages) -> {
        List<AnswerVerifier.Verdict> out = new ArrayList<>();
        for (String p : passages) out.add(new AnswerVerifier.Verdict("DIRECT", p.split("\\. ")[0]));
        return out;
    };

    private static final AnswerVerifier UNAVAILABLE = (q, passages) -> {
        throw new IllegalStateException("offline");
    };

    @Test
    void aHighScoringCertificationsPassageIsNotEvidenceForADpaQuestion() {
        AtomicInteger calls = new AtomicInteger();
        AnswerVerifier counting = (q, p) -> {
            calls.incrementAndGet();
            return AGREES_WITH_EVERYTHING.verify(q, p);
        };
        SearchResult r = result(CERTIFICATIONS_HELD, 0.82);

        new AnswerSupportService(counting).assess(DPA_QUESTION, List.of(r));

        assertThat(r.getRelevanceLevel()).isEqualTo("HIGH");
        assertThat(r.getSupportLevel()).isEqualTo("RELATED");
        assertThat(r.getSupportReason()).contains("DPA");
        assertThat(r.getEvidence()).isEmpty();
        assertThat(calls).hasValue(0); // ruled out before the verifier is asked
    }

    @Test
    void scheduledAuditsAreNotEvidenceOfCurrentlyHoldingACertification() {
        SearchResult r = result(UPCOMING_AUDITS, 0.78);

        new AnswerSupportService(AGREES_WITH_EVERYTHING).assess(ISO_QUESTION, List.of(r));

        assertThat(r.getSupportLevel()).isEqualTo("RELATED");
        assertThat(r.getSupportReason()).contains("audits");
    }

    @Test
    void aVerifiedQuoteFromAPassageThatStatesTheFactIsSupported() {
        SearchResult r = result(CERTIFICATIONS_HELD, 0.74);
        AnswerVerifier verifier = (q, p) -> List.of(new AnswerVerifier.Verdict("DIRECT",
                "Cloudera maintains SOC 2 Type II and ISO/IEC 27001 certification for its data platform"));

        new AnswerSupportService(verifier).assess(ISO_QUESTION, List.of(r));

        assertThat(r.getSupportLevel()).isEqualTo("SUPPORTED");
        assertThat(r.getEvidence()).startsWith("Cloudera maintains SOC 2 Type II and ISO/IEC 27001");
    }

    @Test
    void aDpaPassageCanSupportTheDpaQuestion() {
        SearchResult r = result(DPA_TEMPLATE, 0.71);
        AnswerVerifier verifier = (q, p) -> List.of(new AnswerVerifier.Verdict("DIRECT",
                "Cloudera offers a standard Data Processing Addendum (DPA) to customers."));

        new AnswerSupportService(verifier).assess(DPA_QUESTION, List.of(r));

        assertThat(r.getSupportLevel()).isEqualTo("SUPPORTED");
        assertThat(r.getEvidence()).contains("Data Processing Addendum");
    }

    @Test
    void theVerifierCanStillRejectAPassageTheRulesAllow() {
        SearchResult r = result(CERTIFICATIONS_HELD, 0.74);
        AnswerVerifier verifier = (q, p) -> List.of(new AnswerVerifier.Verdict("NOT_ANSWERED", ""));

        new AnswerSupportService(verifier).assess(ISO_QUESTION, List.of(r));

        assertThat(r.getSupportLevel()).isEqualTo("RELATED");
    }

    @Test
    void aQuoteThatIsNotInThePassageIsNeverShownAsEvidence() {
        SearchResult r = result(CERTIFICATIONS_HELD, 0.74);
        AnswerVerifier invents = (q, p) -> List.of(new AnswerVerifier.Verdict("DIRECT",
                "Cloudera has held ISO 27001 since 2015."));

        new AnswerSupportService(invents).assess(ISO_QUESTION, List.of(r));

        assertThat(r.getSupportLevel()).isEqualTo("PARTIAL");
        assertThat(r.getEvidence()).doesNotContain("since 2015");
    }

    @Test
    void withoutTheVerifierAPassageIsAtMostPartial() {
        SearchResult r = result(CERTIFICATIONS_HELD, 0.90);

        new AnswerSupportService(UNAVAILABLE).assess(ISO_QUESTION, List.of(r));

        assertThat(r.getSupportLevel()).isEqualTo("PARTIAL");
        assertThat(r.getEvidence()).contains("ISO/IEC 27001");
        assertThat(r.getSupportReason()).contains("not independently verified");
    }

    @Test
    void lowScoringMatchesStayRelatedKnowledge() {
        // e.g. the DPA question that matched a certifications summary at 36%
        SearchResult r = result(DPA_TEMPLATE, 0.36);

        new AnswerSupportService(AGREES_WITH_EVERYTHING).assess(DPA_QUESTION, List.of(r));

        assertThat(r.getRelevanceLevel()).isEqualTo("LOW");
        assertThat(r.getSupportLevel()).isEqualTo("RELATED");
    }

    @Test
    void relevanceBandsDescribeTheScoreOnly() {
        assertThat(RetrievalService.relevanceLevel(0.60)).isEqualTo("HIGH");
        assertThat(RetrievalService.relevanceLevel(0.55)).isEqualTo("MEDIUM");
        assertThat(RetrievalService.relevanceLevel(0.36)).isEqualTo("LOW");
    }
}
