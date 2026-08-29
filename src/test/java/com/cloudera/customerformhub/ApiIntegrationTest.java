package com.cloudera.customerformhub;

import com.cloudera.customerformhub.config.DataLoader;
import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.RetrievalEvaluationRun;
import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.RetrievalEvaluationRunRepository;
import com.cloudera.customerformhub.repository.SmeRequestQuestionRepository;
import com.cloudera.customerformhub.repository.SmeRequestRepository;
import com.cloudera.customerformhub.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level checks for the main frontend-to-backend contracts. The application
 * runs against H2 here; the DataLoader is disabled so each test owns its data.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private FormQuestionRepository questionRepository;

    @Autowired
    private SmeRequestRepository smeRequestRepository;

    @Autowired
    private SmeRequestQuestionRepository smeRequestQuestionRepository;

    @Autowired
    private RetrievalEvaluationRunRepository evaluationRunRepository;

    @MockitoBean
    private DataLoader dataLoader;

    @BeforeEach
    void clearDatabase() {
        smeRequestQuestionRepository.deleteAll();
        smeRequestRepository.deleteAll();
        questionRepository.deleteAll();
        ticketRepository.deleteAll();
        evaluationRunRepository.deleteAll();
    }

    @Test
    void createsTicketAndUpdatesOnlyItsStatusThroughTheApi() throws Exception {
        String ticketJson = """
                {
                  "customerName": "Northstar Ltd",
                  "createdBy": "Emma Jones",
                  "assignedTo": "Sarah Chen",
                  "status": "New",
                  "urgency": "High",
                  "ndaStatus": "In Place"
                }
                """;

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.customerName").value("Northstar Ltd"))
                .andExpect(jsonPath("$.status").value("New"))
                .andReturn().getResponse().getContentAsString();

        long ticketId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"In Progress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("In Progress"))
                .andExpect(jsonPath("$.customerName").value("Northstar Ltd"));
    }

    @Test
    void escalatesAnAiSuggestionAndReportsThatOutcomeThroughTheApi() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Blue Harbor", "In Progress"));
        FormQuestion question = questionRepository.save(question(
                ticket.getId(), "Can you confirm your incident response process?", "InfoSec", "Needs Review"));

        mockMvc.perform(post("/api/questions/{id}/review-escalation", question.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"AE\",\"suggestionSourceId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Waiting AE"))
                .andExpect(jsonPath("$.reviewOutcome").value("ESCALATED"))
                .andExpect(jsonPath("$.aiSuggestionSourceId").value(42))
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty())
                .andExpect(jsonPath("$.aeClarificationRequestedAt").isNotEmpty());

        mockMvc.perform(get("/api/ai-performance/review-summary")
                        .param("from", "2020-01-01T00:00:00Z")
                        .param("to", "2030-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewed").value(1))
                .andExpect(jsonPath("$.counts.accepted").value(0))
                .andExpect(jsonPath("$.counts.escalated").value(1))
                .andExpect(jsonPath("$.rates.rejectedOrEscalated").value(100.0));
    }

    @Test
    void dispatchesSmeQuestionsByDepartmentWithoutDuplicatingThePackage() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Cedar Works", "In Progress"));
        questionRepository.save(question(ticket.getId(), "Do you encrypt customer data at rest?", "InfoSec", "SME Needed"));
        questionRepository.save(question(ticket.getId(), "What is your retention policy?", "InfoSec", "SME Needed"));
        questionRepository.save(question(ticket.getId(), "Who can sign the agreement?", "Legal", "SME Needed"));

        mockMvc.perform(post("/api/sme-requests/dispatch/ticket/{ticketId}", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].department").value("InfoSec"))
                .andExpect(jsonPath("$[0].questionCount").value(2))
                .andExpect(jsonPath("$[1].department").value("Legal"))
                .andExpect(jsonPath("$[1].questionCount").value(1));

        mockMvc.perform(post("/api/sme-requests/dispatch/ticket/{ticketId}", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        org.junit.jupiter.api.Assertions.assertEquals(2, smeRequestRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(3, smeRequestQuestionRepository.count());
    }

    @Test
    void exposesCompletedRetrievalRunsAndRejectsInvalidLimits() throws Exception {
        RetrievalEvaluationRun run = new RetrievalEvaluationRun();
        run.setId("api-test-run");
        run.setStatus("COMPLETED");
        run.setStartedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        run.setCompletedAt(LocalDateTime.of(2026, 8, 1, 10, 1));
        run.setDurationMs(1_000L);
        run.setDatasetVersion("synthetic-v1");
        run.setCaseCount(12);
        run.setFailedCount(0);
        run.setSkippedCount(0);
        run.setTop1Hits(10);
        run.setTop3Hits(12);
        evaluationRunRepository.save(run);

        mockMvc.perform(get("/api/ai-performance/retrieval-runs").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].runId").value("api-test-run"))
                .andExpect(jsonPath("$[0].top1HitRate").value(83.3))
                .andExpect(jsonPath("$[0].top3HitRate").value(100.0));

        mockMvc.perform(get("/api/ai-performance/retrieval-runs").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limit must be between 1 and 100"));
    }

    private Ticket ticket(String customerName, String status) {
        return new Ticket(customerName, "Emma Jones", "Sarah Chen", status,
                "Medium", "In Place", null, null, null, null);
    }

    private FormQuestion question(Long ticketId, String text, String department, String status) {
        return new FormQuestion(ticketId, text, department, status, null, null);
    }
}
