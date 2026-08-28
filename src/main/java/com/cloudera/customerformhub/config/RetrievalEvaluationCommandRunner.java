package com.cloudera.customerformhub.config;

import com.cloudera.customerformhub.dto.RetrievalEvaluationRunSummary;
import com.cloudera.customerformhub.service.RetrievalEvaluationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@ConditionalOnProperty(name = "ai-performance.evaluation.run", havingValue = "true")
public class RetrievalEvaluationCommandRunner implements ApplicationRunner {
    private final RetrievalEvaluationService evaluationService;

    public RetrievalEvaluationCommandRunner(RetrievalEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        RetrievalEvaluationRunSummary result = evaluationService.runEvaluation();
        System.out.println(">>> AI Performance retrieval evaluation: " + result.runId()
                + " status=" + result.status()
                + " top1=" + result.top1HitRate()
                + " top3=" + result.top3HitRate());
    }
}
