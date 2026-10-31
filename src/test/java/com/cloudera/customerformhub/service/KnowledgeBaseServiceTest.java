package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class KnowledgeBaseServiceTest {

    private final KnowledgeBaseRepository repository = mock(KnowledgeBaseRepository.class);
    private final EmbeddingService embeddings = mock(EmbeddingService.class);
    private final KnowledgeBaseService service = new KnowledgeBaseService(repository, embeddings);

    private KnowledgeBase stored() {
        KnowledgeBase entry = new KnowledgeBase();
        entry.setId(5L);
        entry.setContent("Data is encrypted with AES-256.");
        entry.setStatus("Pending Review");
        entry.setEmbedding("[0.1,0.2]");
        when(repository.findById(5L)).thenReturn(Optional.of(entry));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return entry;
    }

    private static KnowledgeBase change(String content, String status) {
        KnowledgeBase changes = new KnowledgeBase();
        changes.setContent(content);
        changes.setStatus(status);
        return changes;
    }

    @Test
    void approvingWithoutChangingTheContentKeepsTheEmbedding() {
        stored();
        // The embedding API is down: a status change must still be saved.
        when(embeddings.getEmbedding(anyString())).thenThrow(new RuntimeException("offline"));

        KnowledgeBase saved = service.update(5L, change("Data is encrypted with AES-256.", "Approved"));

        assertThat(saved.getStatus()).isEqualTo("Approved");
        assertThat(saved.getEmbedding()).isEqualTo("[0.1,0.2]");
        verify(embeddings, never()).getEmbedding(anyString());
    }

    @Test
    void newContentGetsANewEmbedding() {
        stored();
        when(embeddings.getEmbedding("Data is encrypted with AES-256 at rest.")).thenReturn(List.of(0.3, 0.4));
        when(embeddings.vectorToString(List.of(0.3, 0.4))).thenReturn("[0.3,0.4]");

        KnowledgeBase saved = service.update(5L, change("Data is encrypted with AES-256 at rest.", "Pending Review"));

        assertThat(saved.getEmbedding()).isEqualTo("[0.3,0.4]");
    }
}
