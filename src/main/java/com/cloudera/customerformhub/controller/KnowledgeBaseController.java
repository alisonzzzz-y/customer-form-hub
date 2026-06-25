package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.dto.SearchRequest;
import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.service.KnowledgeBaseService;
import com.cloudera.customerformhub.service.RetrievalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

// constructor injection, controller needs service
    // 1. declare a field - service, like an empty box
    private final KnowledgeBaseService knowledgeBaseService;
    private final RetrievalService retrievalService;

    // 2. constructor
    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                   RetrievalService retrievalService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.retrievalService = retrievalService;
    }

    // GET http://localhost:8080/api/knowledge-base
    @GetMapping
    public List<KnowledgeBase> getAll() {
        return knowledgeBaseService.findAll();
    }

    @PostMapping("/search")
    public List<KnowledgeBase> search(@RequestBody SearchRequest request) {
        return retrievalService.search(request.getQuestion());
    }
}