package com.cloudera.customerformhub;

import com.cloudera.customerformhub.config.DataLoader;
import com.cloudera.customerformhub.entity.FinalAnswer;
import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.RetrievalEvaluationRun;
import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.FinalAnswerRepository;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.RetrievalEvaluationRunRepository;
import com.cloudera.customerformhub.repository.SmeRequestQuestionRepository;
import com.cloudera.customerformhub.repository.SmeRequestRepository;
import com.cloudera.customerformhub.repository.TicketRepository;
import com.cloudera.customerformhub.service.QuestionClassifierService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private FinalAnswerRepository finalAnswerRepository;

    @MockitoBean
    private DataLoader dataLoader;

    @MockitoBean
    private QuestionClassifierService classifierService;

    @BeforeEach
    void clearDatabase() {
        finalAnswerRepository.deleteAll();
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

    @Test
    void importsEveryQuestionFromAMultiSheetWorkbookExactlyOnce() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Vandelay Industries", "Intake Review"));
        when(classifierService.classify(anyList()))
                .thenAnswer(inv -> Collections.nCopies(((List<?>) inv.getArgument(0)).size(), "InfoSec"));
        MockMultipartFile file = demoWorkbook();

        String first = mockMvc.perform(multipart("/api/questionnaire/import").file(file)
                        .param("ticketId", ticket.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[0].rowReference").value("InfoSec"))
                .andExpect(jsonPath("$[15].rowReference").value("ESG"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(16, questionRepository.findByTicketId(ticket.getId()).size());

        // A reviewer moves one question before the same file is analysed again.
        long movedId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(first).get(0).get("id").asLong();
        mockMvc.perform(put("/api/questions/{id}", movedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"Compliance\"}"))
                .andExpect(status().isOk());

        String second = mockMvc.perform(multipart("/api/questionnaire/import").file(file)
                        .param("ticketId", ticket.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[0].department").value("Compliance"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(16, questionRepository.findByTicketId(ticket.getId()).size());
        assertEquals(idsOf(first), idsOf(second));
        // Only the first import needed the classifier: nothing new was found the second time.
        verify(classifierService, times(1)).classify(anyList());
    }

    @Test
    void keepsQuestionsThatTheCustomerFormGenuinelyRepeats() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Repeat Co", "Intake Review"));
        when(classifierService.classify(anyList()))
                .thenAnswer(inv -> Collections.nCopies(((List<?>) inv.getArgument(0)).size(), "InfoSec"));
        MockMultipartFile file = workbook(new String[][] {
                {"InfoSec", "Is data encrypted in transit?", "Is data encrypted in transit?", "Do you hold ISO 27001?"}
        });

        mockMvc.perform(multipart("/api/questionnaire/import").file(file)
                        .param("ticketId", ticket.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
        mockMvc.perform(multipart("/api/questionnaire/import").file(file)
                        .param("ticketId", ticket.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        assertEquals(3, questionRepository.findByTicketId(ticket.getId()).size());
    }

    @Test
    void rejectsAnImportForATicketThatDoesNotExist() throws Exception {
        mockMvc.perform(multipart("/api/questionnaire/import").file(demoWorkbook())
                        .param("ticketId", "999999"))
                .andExpect(status().isNotFound());

        verify(classifierService, never()).classify(anyList());
        assertEquals(0, questionRepository.count());
    }

    @Test
    void recordsWhichFileWasImportedAndTreatsTheSameFileAsARepeat() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Vandelay Industries", "Intake Review"));
        classifyAllAs("InfoSec");
        MockMultipartFile file = demoWorkbook();

        String first = importFile(ticket, file);
        mockMvc.perform(get("/api/tickets/{id}", ticket.getId()))
                .andExpect(jsonPath("$.formFileName").value("Vandelay_Industries_Demo_Questionnaire.xlsx"))
                .andExpect(jsonPath("$.formImportOutcome").value("CREATED"))
                .andExpect(jsonPath("$.formQuestionsAdded").value(16))
                .andExpect(jsonPath("$.formQuestionsRetired").value(0));

        String second = importFile(ticket, file);
        assertEquals(idsOf(first), idsOf(second));
        mockMvc.perform(get("/api/tickets/{id}", ticket.getId()))
                .andExpect(jsonPath("$.formImportOutcome").value("UNCHANGED"))
                .andExpect(jsonPath("$.formQuestionsAdded").value(0));
        mockMvc.perform(get("/api/questions/ticket/{id}", ticket.getId()))
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[0].departmentSource").value("AI"));
    }

    @Test
    void aNewVersionOfTheFormIsParsedAndKeepsReviewedAnswers() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Version Co", "In Progress"));
        classifyAllAs("InfoSec");
        String v1 = importFile(ticket, workbook(new String[][] {
                {"InfoSec", "Is data encrypted in transit?", "Do you hold ISO 27001?", "Do you run penetration tests?"}
        }, "form-v1.xlsx"));
        List<Long> v1Ids = idsOf(v1);
        long encrypted = v1Ids.get(0);
        long iso = v1Ids.get(1);

        // Both questions were reviewed: one stays in v2, the other was dropped.
        finalAnswerRepository.save(new FinalAnswer(encrypted, null, "TLS 1.2+", false, "Knowledge Base", "Confirmed", "Sarah Chen"));
        finalAnswerRepository.save(new FinalAnswer(iso, null, "Yes, certified.", false, "Knowledge Base", "Confirmed", "Sarah Chen"));
        mockMvc.perform(put("/api/questions/{id}", encrypted)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"Compliance\",\"departmentSource\":\"Manual\"}"))
                .andExpect(status().isOk());

        String v2 = importFile(ticket, workbook(new String[][] {
                {"InfoSec", "Is data encrypted in transit?", "Do you run penetration tests?", "Is MFA enforced for admins?"}
        }, "form-v2.xlsx"));

        List<Long> v2Ids = idsOf(v2);
        assertEquals(3, v2Ids.size());
        assertEquals(encrypted, v2Ids.get(0));      // same row, not re-created
        assertEquals(v1Ids.get(2), v2Ids.get(1));
        mockMvc.perform(get("/api/tickets/{id}", ticket.getId()))
                .andExpect(jsonPath("$.formFileName").value("form-v2.xlsx"))
                .andExpect(jsonPath("$.formImportOutcome").value("UPDATED"))
                .andExpect(jsonPath("$.formQuestionsAdded").value(1))
                .andExpect(jsonPath("$.formQuestionsRetired").value(1));
        mockMvc.perform(get("/api/questions/ticket/{id}", ticket.getId()))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].department").value("Compliance"))
                .andExpect(jsonPath("$[0].departmentSource").value("Manual"));
        // The approved answers are untouched; the dropped question is retired, not deleted.
        assertEquals("TLS 1.2+", finalAnswerRepository.findByQuestionId(encrypted).get(0).getAnswerText());
        assertEquals("Yes, certified.", finalAnswerRepository.findByQuestionId(iso).get(0).getAnswerText());
        assertEquals(true, questionRepository.findById(iso).orElseThrow().getRetiredAt() != null);
        mockMvc.perform(get("/api/final-review/ticket/{id}", ticket.getId()))
                .andExpect(jsonPath("$", hasSize(3)));

        // Going back to v1 brings the retired question back with its answer.
        String v1Again = importFile(ticket, workbook(new String[][] {
                {"InfoSec", "Is data encrypted in transit?", "Do you hold ISO 27001?", "Do you run penetration tests?"}
        }, "form-v1.xlsx"));
        assertEquals(v1Ids, idsOf(v1Again));
        assertEquals(null, questionRepository.findById(iso).orElseThrow().getRetiredAt());
    }

    @Test
    void aFileWithNoQuestionsIsRejectedAndKeepsTheSavedQuestions() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Empty Co", "In Progress"));
        classifyAllAs("InfoSec");
        List<Long> saved = idsOf(importFile(ticket, workbook(new String[][] {
                {"InfoSec", "Is data encrypted in transit?", "Do you hold ISO 27001?"}
        }, "form-v1.xlsx")));
        finalAnswerRepository.save(new FinalAnswer(saved.get(0), null, "TLS 1.2+", false, "Knowledge Base", "Confirmed", "Sarah Chen"));

        // Only the "Question" header, no rows.
        mockMvc.perform(multipart("/api/questionnaire/import")
                        .file(workbook(new String[][] {{"InfoSec"}}, "empty.xlsx"))
                        .param("ticketId", ticket.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not changed")));

        mockMvc.perform(get("/api/questions/ticket/{id}", ticket.getId()))
                .andExpect(jsonPath("$", hasSize(2)));
        assertEquals(null, questionRepository.findById(saved.get(0)).orElseThrow().getRetiredAt());
        mockMvc.perform(get("/api/tickets/{id}", ticket.getId()))
                .andExpect(jsonPath("$.formFileName").value("form-v1.xlsx"))
                .andExpect(jsonPath("$.formImportOutcome").value("CREATED"));
    }

    @Test
    void recordsWhenATicketIsClosedAndClearsItOnReopen() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Close Co", "Approved"));
        String status = "/api/tickets/{id}/status";

        mockMvc.perform(patch(status, ticket.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Closed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
        var closedAt = ticketRepository.findById(ticket.getId()).orElseThrow().getClosedAt();

        // Archiving keeps the close time; reopening clears it.
        mockMvc.perform(patch(status, ticket.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Archived\"}"))
                .andExpect(status().isOk());
        assertEquals(closedAt, ticketRepository.findById(ticket.getId()).orElseThrow().getClosedAt());
        mockMvc.perform(patch(status, ticket.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Ready for Review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedAt").value(nullValue()));
    }

    @Test
    void aRetriedCreateWithTheSameClientRefReturnsTheSameTicket() throws Exception {
        String body = "{\"customerName\":\"Retry Co\",\"status\":\"Intake Review\",\"clientRef\":\"tab-7f3a-1\"}";
        String first = mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        assertEquals(json.readTree(first).get("id").asLong(), json.readTree(second).get("id").asLong());
        assertEquals(1, ticketRepository.count());
    }

    @Test
    void savesTheWorkflowStepAndRejectsUnknownOnes() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Stage Co", "In Progress"));

        mockMvc.perform(put("/api/tickets/{id}", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflowStage\":\"grouping\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStage").value("grouping"));
        mockMvc.perform(put("/api/tickets/{id}", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflowStage\":\"somewhere\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tickets/{id}", ticket.getId()))
                .andExpect(jsonPath("$.workflowStage").value("grouping"));
    }

    @Test
    void preparedSmeDraftIsNotSentUntilAUserConfirmsIt() throws Exception {
        Ticket ticket = ticketRepository.save(ticket("Draft Co", "In Progress"));

        String created = mockMvc.perform(post("/api/sme-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":" + ticket.getId()
                                + ",\"department\":\"Finance\",\"status\":\"Draft Prepared\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Draft Prepared"))
                .andExpect(jsonPath("$.sentAt").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        long requestId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/sme-requests/{id}", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Waiting for ETA\",\"sentAt\":\"2026-10-27T10:15:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Waiting for ETA"))
                .andExpect(jsonPath("$.sentAt").value("2026-10-27T10:15:00"));

        // A second confirmation cannot rewrite when the request was first sent.
        mockMvc.perform(put("/api/sme-requests/{id}", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentAt\":\"2026-10-28T09:00:00\",\"eta\":\"2026-11-03T17:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentAt").value("2026-10-27T10:15:00"))
                .andExpect(jsonPath("$.eta").value("2026-11-03T17:00:00"));
    }

    private String importFile(Ticket ticket, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/questionnaire/import").file(file)
                        .param("ticketId", ticket.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void classifyAllAs(String department) {
        when(classifierService.classify(anyList()))
                .thenAnswer(inv -> Collections.nCopies(((List<?>) inv.getArgument(0)).size(), department));
    }

    private List<Long> idsOf(String json) throws Exception {
        List<Long> ids = new ArrayList<>();
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).forEach(n -> ids.add(n.get("id").asLong()));
        return ids;
    }

    // Same shape as Vandelay_Industries_Demo_Questionnaire.xlsx: five sheets, 16 questions.
    private MockMultipartFile demoWorkbook() throws Exception {
        return workbook(new String[][] {
                {"InfoSec", "Do you currently hold ISO/IEC 27001 certification?",
                        "Describe how customer data is encrypted in transit and at rest.",
                        "What is your process for detecting, reporting, and responding to security incidents?",
                        "How is employee access to customer data granted, reviewed, and revoked?",
                        "Do you carry out regular penetration tests or vulnerability assessments?",
                        "How long are security event logs retained, and who can access them?"},
                {"Legal", "Is a standard Data Processing Agreement (DPA) available for customers?",
                        "In which regions can customer data be stored or processed?",
                        "What is your policy for retaining and deleting customer data after contract termination?",
                        "How are customers notified when you add or change a subprocessor?"},
                {"Finance", "Do you maintain cyber liability insurance, and what is the coverage limit?",
                        "Can you provide your most recent audited financial statements under a suitable NDA?"},
                {"HR", "Do employees complete information security awareness training at least once a year?",
                        "What was your employee turnover rate during the last financial year?"},
                {"ESG", "Do you publish an annual sustainability or environmental performance report?",
                        "Do you measure and report Scope 1 and Scope 2 greenhouse gas emissions?"},
        });
    }

    // Each row of `sheets` is {sheetName, question...}; a blank row is added to check it is skipped.
    private MockMultipartFile workbook(String[][] sheets) throws Exception {
        return workbook(sheets, "Vandelay_Industries_Demo_Questionnaire.xlsx");
    }

    private MockMultipartFile workbook(String[][] sheets, String fileName) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String[] spec : sheets) {
                Sheet sheet = wb.createSheet(spec[0]);
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Question ID");
                header.createCell(1).setCellValue("Question");
                header.createCell(2).setCellValue("Supplier Answer");
                for (int i = 1; i < spec.length; i++) {
                    Row row = sheet.createRow(i);
                    row.createCell(0).setCellValue(String.format("Q%02d", i));
                    row.createCell(1).setCellValue(spec[i]);
                }
                sheet.createRow(spec.length).createCell(1).setCellValue(" ");
            }
            wb.write(out);
            return new MockMultipartFile("file", fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void appendsToExistingSmePackageWithoutDuplicatesOrLosingAnswers() throws Exception {
        var t = ticketRepository.save(ticket("Append fixture", "Waiting SME"));
        var q1 = questionRepository.save(question(t.getId(), "First question?", "InfoSec", "SME Needed"));
        var request = new com.cloudera.customerformhub.entity.SmeRequest();
        request.setTicketId(t.getId());
        request.setDepartment("InfoSec");
        request.setStatus("Draft Prepared");
        request = smeRequestRepository.save(request);
        String body = "{\"smeRequestId\":" + request.getId() + ",\"ticketId\":" + t.getId() + ",\"department\":\"InfoSec\"}";
        mockMvc.perform(post("/api/sme-request-questions/package").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        var oldLink = smeRequestQuestionRepository.findBySmeRequestId(request.getId()).get(0);
        oldLink.setReturnedAnswer("Keep this original answer");
        oldLink.setStatus("Returned");
        smeRequestQuestionRepository.save(oldLink);
        q1.setStatus("Answered");
        questionRepository.save(q1);
        request.setStatus("ETA Confirmed");
        request.setSentAt(LocalDateTime.now());
        request.setEta(LocalDateTime.now().plusDays(2));
        smeRequestRepository.save(request);
        var sentAt = request.getSentAt();
        var eta = request.getEta();
        var q2 = questionRepository.save(question(t.getId(), "Second question?", "InfoSec", "SME Needed"));

        // The sent request keeps its send record and ETA: it does not take new questions.
        mockMvc.perform(post("/api/sme-request-questions/package").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        var unchanged = smeRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals("ETA Confirmed", unchanged.getStatus());
        assertEquals(sentAt.withNano(0), unchanged.getSentAt().withNano(0));
        assertEquals(eta.withNano(0), unchanged.getEta().withNano(0));
        assertEquals(1, smeRequestQuestionRepository.findBySmeRequestId(request.getId()).size());
        assertEquals("Keep this original answer", smeRequestQuestionRepository.findById(oldLink.getId()).orElseThrow().getReturnedAnswer());

        // The new question goes into a new draft request, once, however often it is packaged.
        var second = new com.cloudera.customerformhub.entity.SmeRequest();
        second.setTicketId(t.getId());
        second.setDepartment("InfoSec");
        second.setStatus("Draft Prepared");
        second = smeRequestRepository.save(second);
        String secondBody = "{\"smeRequestId\":" + second.getId() + ",\"ticketId\":" + t.getId() + ",\"department\":\"InfoSec\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/sme-request-questions/package").contentType(MediaType.APPLICATION_JSON).content(secondBody))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].questionId").value(q2.getId()));
        }
        assertEquals(1, smeRequestRepository.findById(second.getId()).orElseThrow().getQuestionCount());
    }

    @Test
    void anUnsentDraftTakesLaterQuestionsAndAQuestionIsNeverPackagedTwice() throws Exception {
        var t = ticketRepository.save(ticket("Draft append", "Waiting SME"));
        questionRepository.save(question(t.getId(), "First?", "Legal", "SME Needed"));
        var draft = new com.cloudera.customerformhub.entity.SmeRequest();
        draft.setTicketId(t.getId());
        draft.setDepartment("Legal");
        draft.setStatus("Draft Prepared");
        draft = smeRequestRepository.save(draft);
        String body = "{\"smeRequestId\":" + draft.getId() + ",\"ticketId\":" + t.getId() + ",\"department\":\"Legal\"}";
        mockMvc.perform(post("/api/sme-request-questions/package").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        questionRepository.save(question(t.getId(), "Second?", "Legal", "SME Needed"));
        mockMvc.perform(post("/api/sme-request-questions/package").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));

        // Dispatch finds nothing new: both are already waiting in the open draft.
        mockMvc.perform(post("/api/sme-requests/dispatch/ticket/{id}", t.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        assertEquals(1, smeRequestRepository.count());
        assertEquals(2, smeRequestQuestionRepository.count());
    }

    @Test
    void previewsRealReturnedWorkbookThroughHttpWithoutSavingAnswers() throws Exception {
        var t = ticketRepository.save(ticket("Import fixture", "Waiting SME"));
        var q = questionRepository.save(question(t.getId(), "Actual question?", "Legal", "SME Needed"));
        var request = new com.cloudera.customerformhub.entity.SmeRequest();
        request.setTicketId(t.getId()); request.setDepartment("Legal");
        request = smeRequestRepository.save(request);
        var link = new com.cloudera.customerformhub.entity.SmeRequestQuestion(request.getId(), q.getId(), "Pending", null, null);
        smeRequestQuestionRepository.save(link);
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Legal");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Question");
            header.createCell(1).setCellValue("SME Answer");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(q.getQuestionText());
            row.createCell(1).setCellValue("Real answer from the uploaded workbook");
            workbook.write(out);
            var file = new MockMultipartFile("file", "returned.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
            mockMvc.perform(multipart("/api/sme-request-questions/request/" + request.getId() + "/import-preview").file(file))
                    .andExpect(status().isOk()).andExpect(jsonPath("$['" + q.getId() + "']").value("Real answer from the uploaded workbook"));
        }
        assertEquals(0, finalAnswerRepository.count());
        mockMvc.perform(multipart("/api/sme-request-questions/request/" + request.getId() + "/import-preview")
                        .file(new MockMultipartFile("file", "invalid.xlsx", "", "invalid".getBytes())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reopeningReviewAlsoMakesTheSavedAnswerADraft() throws Exception {
        var t = ticketRepository.save(ticket("Undo fixture", "In Progress"));
        var q = questionRepository.save(question(t.getId(), "Question to reopen?", "Legal", "Answered"));
        var answer = new FinalAnswer();
        answer.setQuestionId(q.getId());
        answer.setAnswerText("Previously approved");
        answer.setApprovalStatus("Confirmed");
        finalAnswerRepository.save(answer);
        mockMvc.perform(post("/api/questions/" + q.getId() + "/review-reopen")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Needs Review"));
        assertEquals("Draft", finalAnswerRepository.findByQuestionId(q.getId()).get(0).getApprovalStatus());
    }

    private Ticket ticket(String customerName, String status) {
        return new Ticket(customerName, "Emma Jones", "Sarah Chen", status,
                "Medium", "In Place", null, null, null, null);
    }

    private FormQuestion question(Long ticketId, String text, String department, String status) {
        return new FormQuestion(ticketId, text, department, status, null, null);
    }
}
