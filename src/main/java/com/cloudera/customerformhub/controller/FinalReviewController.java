package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.dto.QuestionWithAnswer;
import com.cloudera.customerformhub.service.FinalReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/final-review")
public class FinalReviewController {

    private final FinalReviewService finalReviewService;

    public FinalReviewController(FinalReviewService finalReviewService) {
        this.finalReviewService = finalReviewService;
    }

    // GET /api/final-review/ticket/{ticketId}
    // → every question for the ticket, each with its final answer (if any)
    @GetMapping("/ticket/{ticketId}")
    public List<QuestionWithAnswer> getReview(@PathVariable Long ticketId) {
        return finalReviewService.getReviewForTicket(ticketId);
    }
}