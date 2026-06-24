package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    // GET http://localhost:8080/api/knowledge-base
    @GetMapping
    public List<KnowledgeBase> getAll() {
        return knowledgeBaseService.findAll();
    }
}