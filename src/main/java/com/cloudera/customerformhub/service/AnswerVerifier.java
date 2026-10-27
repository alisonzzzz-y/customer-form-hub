package com.cloudera.customerformhub.service;

import java.util.List;

// Independent check of whether each passage answers a question.
public interface AnswerVerifier {

    // DIRECT: the passage states the answer. PARTIAL: it answers part of it.
    // NOT_ANSWERED: related topic only. FUTURE_ONLY: plans or upcoming events,
    // not the current state asked about. evidence is copied from the passage.
    record Verdict(String verdict, String evidence) {
    }

    // One verdict per passage, in order. Throws if the check cannot run.
    List<Verdict> verify(String question, List<String> passages);
}
