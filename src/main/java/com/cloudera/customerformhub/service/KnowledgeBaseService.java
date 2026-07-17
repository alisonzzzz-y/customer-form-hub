package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class KnowledgeBaseService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "Draft", "Pending Review", "Approved", "Deprecated", "Archived"
    );

    // constructor injection
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EmbeddingService embeddingService;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository,
                                EmbeddingService embeddingService) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.embeddingService = embeddingService;
    }

    // get all records from knowledge base
    public List<KnowledgeBase> findAll() {
        return knowledgeBaseRepository.findAll();
    }

    // get one record by id (null if not found)
    public KnowledgeBase findById(Long id) {
        return knowledgeBaseRepository.findById(id).orElse(null);
    }

    // create a new knowledge chunk, generating its embedding from the content
    public KnowledgeBase create(KnowledgeBase chunk) {
        normaliseLifecycle(chunk);
        regenerateEmbedding(chunk);
        return knowledgeBaseRepository.save(chunk);
    }

    // update an existing chunk; regenerate embedding because the content may have changed
    public KnowledgeBase update(Long id, KnowledgeBase updated) {
        KnowledgeBase existing = knowledgeBaseRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        // copy the editable fields onto the existing row
        existing.setDocumentTitle(updated.getDocumentTitle());
        existing.setSectionTitle(updated.getSectionTitle());
        existing.setContent(updated.getContent());
        existing.setSource(updated.getSource());
        existing.setLastUpdated(updated.getLastUpdated());
        existing.setSharingStatus(updated.getSharingStatus());
        existing.setDepartment(updated.getDepartment());
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        } else if (updated.getApproved() != null) {
            // Compatibility with clients that still send only approved=true/false.
            existing.setStatus(Boolean.TRUE.equals(updated.getApproved())
                    ? "Approved" : "Pending Review");
        }
        normaliseLifecycle(existing);

        // content may have changed, so regenerate the embedding
        regenerateEmbedding(existing);
        return knowledgeBaseRepository.save(existing);
    }

    private void normaliseLifecycle(KnowledgeBase chunk) {
        String status = chunk.getStatus();
        if (status == null || status.isBlank()) {
            status = Boolean.TRUE.equals(chunk.getApproved()) ? "Approved" : "Pending Review";
        }
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid knowledge status: " + status);
        }
        chunk.setStatus(status);
        chunk.setApproved("Approved".equals(status));
    }

    // Generate the embedding from the chunk's content and store it as a JSON string.
    // Called on create and update so the vector always matches the current content.
    private void regenerateEmbedding(KnowledgeBase chunk) {
        if (chunk.getContent() == null || chunk.getContent().isBlank()) {
            chunk.setEmbedding(null);
            return;
        }
        List<Double> vector = embeddingService.getEmbedding(chunk.getContent());
        chunk.setEmbedding(embeddingService.vectorToString(vector));
    }
}
