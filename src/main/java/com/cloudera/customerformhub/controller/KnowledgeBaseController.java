package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.dto.SearchRequest;
import com.cloudera.customerformhub.dto.SearchResult;
import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.service.KnowledgeBaseService;
import com.cloudera.customerformhub.service.RetrievalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

    // constructor injection, controller needs service
    private final KnowledgeBaseService knowledgeBaseService;
    private final RetrievalService retrievalService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                   RetrievalService retrievalService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.retrievalService = retrievalService;
    }

    // GET /api/knowledge-base  → all chunks
    @GetMapping
    public List<KnowledgeBase> getAll() {
        return knowledgeBaseService.findAll();
    }

    // GET /api/knowledge-base/{id}  → one chunk
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBase> getById(@PathVariable Long id) {
        KnowledgeBase chunk = knowledgeBaseService.findById(id);
        if (chunk == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chunk);
    }

    // POST /api/knowledge-base  → create a chunk (embedding generated automatically)
    @PostMapping
    public KnowledgeBase create(@RequestBody KnowledgeBase chunk) {
        return knowledgeBaseService.create(chunk);
    }

    // POST /api/knowledge-base/search  → semantic search
    @PostMapping("/search")
    public List<SearchResult> search(@RequestBody SearchRequest request) {
        return retrievalService.search(request.getQuestion());
    }

    // PUT /api/knowledge-base/{id}  → update a chunk (embedding regenerated automatically)
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBase> update(@PathVariable Long id, @RequestBody KnowledgeBase chunk) {
        KnowledgeBase saved = knowledgeBaseService.update(id, chunk);
        if (saved == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(saved);
    }
}