package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    // constructor injection
    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    // get all records from knowledge base
    public List<KnowledgeBase> findAll() {
        return knowledgeBaseRepository.findAll();
    }
}