package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.DashboardStats;
import com.cloudera.customerformhub.entity.FinalAnswer;
import com.cloudera.customerformhub.entity.SmeRequest;
import com.cloudera.customerformhub.repository.FinalAnswerRepository;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final FormQuestionRepository questionRepository;
    private final FinalAnswerRepository answerRepository;
    private final SmeRequestService smeRequestService;

    public DashboardService(TicketRepository ticketRepository,
                            FormQuestionRepository questionRepository,
                            FinalAnswerRepository answerRepository,
                            SmeRequestService smeRequestService) {
        this.ticketRepository = ticketRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.smeRequestService = smeRequestService;
    }

    // Aggregate read-only dashboard metrics from existing data. Small dataset,
    // so in-memory aggregation is fine and keeps this decoupled from custom queries.
    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();

        stats.setTotalTickets(ticketRepository.count());
        stats.setTotalQuestions(questionRepository.count());

        // Ticket status buckets
        long closed = ticketRepository.findAll().stream()
                .filter(t -> "Closed".equals(t.getStatus()) || "Archived".equals(t.getStatus()))
                .count();
        stats.setClosedTickets(closed);
        stats.setInProgressTickets(stats.getTotalTickets() - closed);

        // Confirmed answers, split by whether the AI (knowledge base) answered
        List<FinalAnswer> answers = answerRepository.findAll();
        long fromKb = answers.stream()
                .filter(a -> "Confirmed".equals(a.getApprovalStatus()))
                .filter(a -> "Knowledge Base".equals(a.getSourceType()))
                .count();
        long confirmedTotal = answers.stream()
                .filter(a -> "Confirmed".equals(a.getApprovalStatus()))
                .count();
        stats.setAnsweredFromKnowledgeBase(fromKb);
        stats.setAnsweredBySme(confirmedTotal - fromKb);

        // Overdue SME requests — reuse the existing overdue logic via the service
        // so the dashboard never disagrees with the SME screens.
        long overdue = smeRequestService.getAllSmeRequests().stream()
                .filter(r -> "Overdue".equals(r.getStatus()))
                .count();
        stats.setOverdueSmeRequests(overdue);

        return stats;
    }
}