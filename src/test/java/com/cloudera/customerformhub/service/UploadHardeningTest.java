package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadHardeningTest {

    @Test
    void knowledgeBaseJsonDoesNotExposeEmbedding() throws Exception {
        KnowledgeBase chunk = new KnowledgeBase();
        chunk.setContent("content");
        chunk.setEmbedding("[0.1,0.2,0.3]");

        String json = new ObjectMapper().writeValueAsString(chunk);

        assertThat(json).doesNotContain("embedding", "0.1");
    }

    @Test
    void damagedDocxIsRejectedAsUnprocessableEntity() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "damaged.docx", "application/octet-stream", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> new DocxParserService().extractText(file))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(422));
    }

    @Test
    void damagedXlsxIsRejectedAsUnprocessableEntity() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "damaged.xlsx", "application/octet-stream", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> new QuestionnaireParserService().parse(file))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(422));
    }
}
