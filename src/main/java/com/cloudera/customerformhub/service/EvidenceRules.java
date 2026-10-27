package com.cloudera.customerformhub.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

// Deterministic checks of whether a passage could answer a question at all.
// They never prove an answer is right; they rule out passages that are only
// about a related topic, so a high similarity score cannot make them look
// like evidence:
//  - the passage must name every specific identifier the question asks about
//    (DPA, ISO 27001, SOC 2, ...), and share enough of its key terms;
//  - a question about the current state ("do you hold / have ...") needs a
//    sentence that states that state now, not one about audits or plans.
final class EvidenceRules {

    record Finding(boolean candidate, String sentence, String reason) {
    }

    // Different spellings of the same thing compare equal after this.
    private static final List<Alias> ALIASES = List.of(
            new Alias("data processing (agreement|addendum)s?", "dpa"),
            new Alias("iso\\s*/?\\s*(iec\\s*)?27001", "iso27001"),
            new Alias("iso\\s*/?\\s*(iec\\s*)?27701", "iso27701"),
            new Alias("iso\\s*/?\\s*(iec\\s*)?27017", "iso27017"),
            new Alias("iso\\s*/?\\s*(iec\\s*)?27018", "iso27018"),
            new Alias("soc\\s*2", "soc2"),
            new Alias("soc\\s*1", "soc1"),
            new Alias("general data protection regulation", "gdpr"),
            new Alias("california consumer privacy act", "ccpa"),
            new Alias("pci[\\s-]*dss", "pci"),
            new Alias("data protection impact assessment", "dpia"),
            new Alias("multi[\\s-]*factor authentication", "mfa"),
            new Alias("single[\\s-]*sign[\\s-]*on", "sso"));

    // Identifiers a passage must name to answer a question that names them.
    private static final Set<String> IDENTIFIERS = Set.of(
            "dpa", "dpia", "gdpr", "ccpa", "hipaa", "pci", "fedramp", "soc1", "soc2",
            "iso27001", "iso27701", "iso27017", "iso27018", "nist", "tisax", "sla", "mfa", "sso");

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "for", "to", "in", "on", "at", "by", "as", "with",
            "from", "into", "about", "is", "are", "was", "were", "be", "been", "being", "do", "does",
            "did", "you", "your", "yours", "we", "our", "us", "it", "its", "this", "that", "these",
            "those", "there", "their", "any", "all", "can", "could", "would", "should", "will", "may",
            "please", "provide", "describe", "explain", "detail", "details", "list", "confirm", "what",
            "which", "who", "how", "when", "where", "why", "if", "have", "has", "had", "hold", "holds",
            "currently", "current", "available", "company", "organisation", "organization", "cloudera",
            "yes", "no", "not", "other", "such", "also", "each", "per", "via", "including", "include");

    // "Do you hold / have / maintain ...", "Are you certified ...": the answer
    // must be a present fact.
    private static final Pattern STATUS_QUESTION = Pattern.compile(
            "\\b(currently|do you (hold|have|maintain|possess)|does (your|the) \\w+ (hold|have|maintain)"
                    + "|are you (certified|compliant|accredited|registered)|is your \\w+ (certified|compliant))\\b");

    private static final Pattern FUTURE = Pattern.compile(
            "\\b(upcoming|planned|plans? to|will|scheduled|expected|expects?|intends?|roadmap|pursuing"
                    + "|working towards?|aims? to|targets?|targeting|anticipated?|next|in progress|underway"
                    + "|not yet|pending|renewals?|recertification|surveillance)\\b");

    private static final Pattern PRESENT = Pattern.compile(
            "\\b(holds?|maintains?|is maintained|are maintained|is certified|are certified"
                    + "|(has|have) (achieved|obtained|been certified|been awarded|a|an)|currently (holds?|maintains?|has|is|are)"
                    + "|is in place|are in place|is compliant|are compliant|operates in accordance|we have"
                    + "|is available|are available|provides?|offers?|signs?)\\b");

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?;])\\s+|\\n+");

    private EvidenceRules() {
    }

    static Finding check(String question, String passage) {
        String q = normalise(question);
        List<String> sentences = new ArrayList<>();
        for (String s : SENTENCE_END.split(passage == null ? "" : passage.trim())) {
            if (!s.isBlank()) sentences.add(s.trim());
        }
        if (sentences.isEmpty()) return new Finding(false, "", "The source is empty.");

        Set<String> passageTokens = tokens(normalise(passage));
        for (String id : identifiers(q)) {
            if (!passageTokens.contains(id)) {
                return new Finding(false, "",
                        "The source does not mention " + id.toUpperCase(Locale.ROOT) + ", which the question asks about.");
            }
        }

        Set<String> terms = stems(keyTerms(q));
        if (!terms.isEmpty() && coverage(terms, stems(passageTokens)) < 0.34) {
            return new Finding(false, "", "The source shares too few key terms with the question.");
        }

        // Sentences about what was asked: those naming the question's
        // identifiers, otherwise those sharing the most key terms.
        Set<String> ids = identifiers(q);
        double best = 0;
        for (String s : sentences) best = Math.max(best, coverage(terms, stems(tokens(normalise(s)))));
        List<String> relevant = new ArrayList<>();
        for (String s : sentences) {
            Set<String> sTokens = tokens(normalise(s));
            boolean named = !ids.isEmpty() && sTokens.containsAll(ids);
            double c = coverage(terms, stems(sTokens));
            if (ids.isEmpty() ? (terms.isEmpty() || (c > 0 && c >= best * 0.75)) : named) relevant.add(s);
        }
        if (relevant.isEmpty()) relevant.add(sentences.get(0));

        boolean askingAboutPlans = FUTURE.matcher(q).find();
        if (!askingAboutPlans && STATUS_QUESTION.matcher(q).find()) {
            for (String s : relevant) {
                String n = normalise(s);
                if (PRESENT.matcher(n).find()) return new Finding(true, s, "");
            }
            boolean future = relevant.stream().anyMatch(s -> FUTURE.matcher(normalise(s)).find());
            return new Finding(false, "", future
                    ? "The source only describes audits, renewals or plans, not the current status the question asks about."
                    : "The source does not state the current status the question asks about.");
        }

        for (String s : relevant) {
            String n = normalise(s);
            if (askingAboutPlans || !FUTURE.matcher(n).find() || PRESENT.matcher(n).find()) {
                return new Finding(true, s, "");
            }
        }
        return new Finding(false, "", "The source only describes plans or upcoming work.");
    }

    static String normalise(String text) {
        String n = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (Alias alias : ALIASES) n = alias.pattern().matcher(n).replaceAll(alias.canonical());
        return n.replaceAll("\\s+", " ").trim();
    }

    private static Set<String> tokens(String normalised) {
        Set<String> out = new LinkedHashSet<>();
        for (String t : normalised.split("[^a-z0-9]+")) if (!t.isEmpty()) out.add(t);
        return out;
    }

    private static Set<String> identifiers(String normalised) {
        Set<String> out = new LinkedHashSet<>();
        for (String t : tokens(normalised)) if (IDENTIFIERS.contains(t)) out.add(t);
        return out;
    }

    private static Set<String> keyTerms(String normalised) {
        Set<String> out = new LinkedHashSet<>();
        for (String t : tokens(normalised)) {
            if (t.length() >= 3 && !STOPWORDS.contains(t)) out.add(t);
        }
        return out;
    }

    // Crude stem so "certified", "certification" and "certificate" compare equal.
    private static Set<String> stems(Set<String> words) {
        Set<String> out = new LinkedHashSet<>();
        for (String w : words) {
            boolean identifier = IDENTIFIERS.contains(w) || w.matches(".*\\d.*");
            out.add(!identifier && w.length() > 6 ? w.substring(0, 6) : w);
        }
        return out;
    }

    private static double coverage(Set<String> terms, Set<String> in) {
        if (terms.isEmpty()) return 1;
        long hit = terms.stream().filter(in::contains).count();
        return (double) hit / terms.size();
    }

    private record Alias(Pattern pattern, String canonical) {
        Alias(String regex, String canonical) {
            this(Pattern.compile("\\b" + regex + "\\b"), canonical);
        }
    }
}
