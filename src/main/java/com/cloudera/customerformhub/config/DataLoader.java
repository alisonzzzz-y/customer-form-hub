package com.cloudera.customerformhub.config;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.entity.SmeRequest;
import com.cloudera.customerformhub.repository.SmeRequestRepository;
import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.entity.RetrievalEvaluationRun;
import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.KnowledgeBaseRepository;
import com.cloudera.customerformhub.repository.RetrievalEvaluationRunRepository;
import com.cloudera.customerformhub.repository.TicketRepository;
import com.cloudera.customerformhub.service.EmbeddingService;
import com.cloudera.customerformhub.service.TicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(0)
public class DataLoader implements CommandLineRunner {

    private final KnowledgeBaseRepository repository;
    private final EmbeddingService embeddingService;
    private final TicketRepository ticketRepository;
    private final SmeRequestRepository smeRequestRepository;
    private final FormQuestionRepository formQuestionRepository;
    private final RetrievalEvaluationRunRepository retrievalEvaluationRunRepository;
    private final boolean refreshDemoData;
    private final boolean rollingDemoDates;

    public DataLoader(KnowledgeBaseRepository repository, EmbeddingService embeddingService,
                      TicketRepository ticketRepository, SmeRequestRepository smeRequestRepository,
                      FormQuestionRepository formQuestionRepository,
                      RetrievalEvaluationRunRepository retrievalEvaluationRunRepository,
                      @Value("${demo-data.refresh:false}") boolean refreshDemoData,
                      @Value("${demo-data.rolling-dates:false}") boolean rollingDemoDates) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.ticketRepository = ticketRepository;
        this.smeRequestRepository = smeRequestRepository;
        this.formQuestionRepository = formQuestionRepository;
        this.retrievalEvaluationRunRepository = retrievalEvaluationRunRepository;
        this.refreshDemoData = refreshDemoData;
        this.rollingDemoDates = rollingDemoDates;
    }

    private LocalDateTime demoDate(int daysFromToday) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(daysFromToday).atStartOfDay();
    }

    // Knowledge-base examples keep fixed document revision dates. Ticket and
    // SME demo dates use demoDate() so they remain useful over time.
    private LocalDateTime d(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay();
    }

    @Override
    public void run(String... args) {
        // Seed the knowledge base chunks if the table is empty
        if (repository.count() == 0) {
            seedData();
        }

        // Backfill the lifecycle column for databases created before status
        // was added. Approved rows remain Approved; all others enter review.
        repository.findAll().forEach(chunk -> {
            if (chunk.getStatus() == null || chunk.getStatus().isBlank()
                    || chunk.getSourceKey() == null || chunk.getSourceKey().isBlank()) {
                chunk.synchroniseLifecycleState();
                repository.save(chunk);
            }
        });

        // Migrate only the legacy ticket values we explicitly understand.
        // Unknown values are left untouched so one dirty row can never stop
        // application startup or break GET /api/tickets.
        backfillTicketStatuses();

        // Seed tickets first; the other seeds depend on the Globex ticket's real id
        if (ticketRepository.count() == 0) {
            Long globexTicketId = seedTickets();

            if (smeRequestRepository.count() == 0) {
                seedSmeRequests(globexTicketId);
            }
            if (formQuestionRepository.count() == 0) {
                seedFormQuestions(globexTicketId);
            }
        }

        if (refreshDemoData || rollingDemoDates) {
            refreshDemoData();
        }

        seedDemoReviewOutcomes();
        seedDemoRetrievalBaseline();

        // Generate embeddings for chunks without one (existing ones are skipped)
        embeddingService.generateEmbeddingsForAll();
    }

    /**
     * Keeps the public demo useful after it has been deployed for a while.
     * This is disabled unless DEMO_DATA_ROLLING_DATES is explicitly enabled.
     */
    @Scheduled(cron = "${demo-data.rolling-cron:0 5 0 * * *}", zone = "UTC")
    public void refreshRollingDemoDates() {
        if (rollingDemoDates) {
            refreshDemoData();
        }
    }

    /**
     * Gives a fresh demo database a visible, synthetic review distribution.
     * It only touches the seeded Globex questions and never overwrites a
     * database that already contains a reviewer outcome.
     */
    private void seedDemoReviewOutcomes() {
        if (formQuestionRepository.findAll().stream()
                .anyMatch(question -> question.getReviewOutcome() != null && !question.getReviewOutcome().isBlank())) {
            return;
        }

        List<FormQuestion> demoQuestions = formQuestionRepository.findAll().stream()
                .filter(question -> question.getRowReference() != null && question.getRowReference().matches("Q[1-8]"))
                .sorted(Comparator.comparingInt(question -> Integer.parseInt(question.getRowReference().substring(1))))
                .toList();
        List<Long> approvedSourceIds = repository.findAll().stream()
                .filter(chunk -> "Approved".equals(chunk.getStatus()) && Boolean.TRUE.equals(chunk.getApproved()))
                .map(KnowledgeBase::getId)
                .filter(id -> id != null)
                .toList();

        if (demoQuestions.size() != 8 || approvedSourceIds.size() < 8) {
            return;
        }

        String[] outcomes = {"ACCEPTED", "ACCEPTED", "ACCEPTED", "ACCEPTED", "ACCEPTED", "EDITED", "EDITED", "ESCALATED"};
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        for (int index = 0; index < demoQuestions.size(); index++) {
            FormQuestion question = demoQuestions.get(index);
            LocalDateTime reviewedAt = nowUtc.minusDays(14L - index);
            question.setAiSuggestionSourceId(approvedSourceIds.get(index));
            question.setReviewOutcome(outcomes[index]);
            question.setReviewedAt(reviewedAt);
            if ("ESCALATED".equals(outcomes[index])) {
                question.setAeClarificationRequestedAt(reviewedAt.minusHours(2));
            }
        }
        formQuestionRepository.saveAll(demoQuestions);
        System.out.println(">>> DataLoader: seeded 8 synthetic AI review outcomes (5 accepted, 2 edited, 1 escalated).");
    }

    /**
     * The public demo needs one visible benchmark result before a user runs an
     * evaluation. This is deliberately synthetic and only exists when the
     * rolling demo-data mode is enabled; a real completed run takes precedence
     * as soon as one is recorded.
     */
    private void seedDemoRetrievalBaseline() {
        if (!rollingDemoDates || retrievalEvaluationRunRepository.existsById("demo-retrieval-v1-baseline")) {
            return;
        }

        LocalDateTime completedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2);
        RetrievalEvaluationRun baseline = new RetrievalEvaluationRun();
        baseline.setId("demo-retrieval-v1-baseline");
        baseline.setStatus("COMPLETED");
        baseline.setStartedAt(completedAt.minusSeconds(2));
        baseline.setCompletedAt(completedAt);
        baseline.setDurationMs(1840L);
        baseline.setDatasetVersion("retrieval-v1-synthetic-demo");
        baseline.setDatasetHash("synthetic-demo-baseline");
        baseline.setCaseCount(12);
        baseline.setFailedCount(0);
        baseline.setSkippedCount(0);
        baseline.setTop1Hits(10);
        baseline.setTop3Hits(12);
        retrievalEvaluationRunRepository.save(baseline);
        System.out.println(">>> DataLoader: seeded one synthetic retrieval baseline for the public demo.");
    }

    private void backfillTicketStatuses() {
        ticketRepository.findAll().forEach(ticket -> {
            String current = ticket.getStatus();
            try {
                String normalised = TicketService.normaliseStatus(current);
                if (!normalised.equals(current)) {
                    ticket.setStatus(normalised);
                    ticketRepository.save(ticket);
                }
            } catch (IllegalArgumentException ex) {
                System.err.println(">>> DataLoader: leaving unknown ticket status unchanged for ticket id "
                        + ticket.getId() + ": " + current);
            }
        });
    }

    // The seeded demo tickets. clientRef marks each row as demo data, so the
    // rolling refresh can find it again and never touches tickets people create.
    record DemoTicket(String clientRef, String customerName, String createdBy, String assignedTo,
                      String status, String urgency, String ndaStatus, int dueOffset,
                      String businessImpact, Integer etaOffset, int createdOffset) {
    }

    static final List<DemoTicket> DEMO_TICKETS = List.of(
            new DemoTicket("demo-seed-acme", "Acme Corp", "—", "Unassigned", "New",
                    "Medium", "Yes", 7, "New logo, evaluating platform", null, -5),
            new DemoTicket("demo-seed-globex", "Globex Inc", "Jane Smith", "Sarah", "Intake Review",
                    "High", "Unknown", 0, "Renewal, medium value", 2, -4),
            new DemoTicket("demo-seed-initech", "Initech", "—", "Sarah", "In Progress",
                    "Medium", "Yes", 4, "Expansion opportunity", 3, -3),
            new DemoTicket("demo-seed-umbrella", "Umbrella Co", "—", "Alex", "Waiting SME",
                    "High", "Yes", -2, "Strategic account renewal", 1, -7));

    // The seeded SME requests of the Globex ticket, one per department.
    record DemoSmeRequest(String department, String teamName, int questionCount,
                          Integer etaOffset, int etaHour, String status, String confirmedBy) {
    }

    static final List<DemoSmeRequest> DEMO_SME_REQUESTS = List.of(
            new DemoSmeRequest("InfoSec", "InfoSec Team", 12, 1, 15, "ETA Confirmed", "Confirmed via email by Alex"),
            new DemoSmeRequest("Legal", "Legal Team", 5, null, 0, "Waiting for ETA", null),
            new DemoSmeRequest("HR", "HR Ops", 8, 2, 17, "ETA Confirmed", "Confirmed by HR lead"),
            new DemoSmeRequest("Finance", "Finance Team", 6, -1, 14, "Overdue", null),
            new DemoSmeRequest("ESG", "ESG Team", 5, 4, 12, "In Progress", null));

    private LocalDateTime demoEta(Integer offset, int hour) {
        return offset == null ? null : demoDate(offset).withHour(hour);
    }

    private Long seedTickets() {
        Long globexId = null;
        for (DemoTicket demo : DEMO_TICKETS) {
            Ticket ticket = new Ticket(
                    demo.customerName(), demo.createdBy(), demo.assignedTo(), demo.status(),
                    demo.urgency(), demo.ndaStatus(), demoDate(demo.dueOffset()),
                    demo.businessImpact(), demoEta(demo.etaOffset(), 0), demoDate(demo.createdOffset()));
            ticket.setClientRef(demo.clientRef());
            ticket = ticketRepository.save(ticket);
            if ("demo-seed-globex".equals(demo.clientRef())) globexId = ticket.getId();
        }

        System.out.println(">>> DataLoader: inserted " + ticketRepository.count() + " tickets.");

        // Return the real, database-generated id of the Globex ticket
        return globexId;
    }

    private void seedSmeRequests(Long globexTicketId) {
        // These SME requests belong to the Globex Inc ticket.
        for (DemoSmeRequest demo : DEMO_SME_REQUESTS) {
            smeRequestRepository.save(new SmeRequest(
                    globexTicketId, demo.department(), demo.teamName(), demo.questionCount(),
                    demoEta(demo.etaOffset(), demo.etaHour()), demo.status(), demo.confirmedBy(),
                    demoDate(-2).withHour(10), null));
        }

        System.out.println(">>> DataLoader: inserted " + smeRequestRepository.count() + " SME requests.");
    }

    /**
     * Moves the seeded demo tickets and their SME requests back to their seeded
     * state with dates relative to today. Only rows the seeder created are
     * touched: tickets people create, and demo tickets someone archived, keep
     * everything they have. The related properties are off by default.
     */
    void refreshDemoData() {
        Map<String, Ticket> demoTickets = findDemoTickets();
        for (DemoTicket demo : DEMO_TICKETS) {
            Ticket ticket = demoTickets.get(demo.clientRef());
            if (ticket == null || "Archived".equals(ticket.getStatus())) continue;
            ticket.setStatus(demo.status());
            ticket.setAssignedTo(demo.assignedTo());
            ticket.setDeadline(demoDate(demo.dueOffset()));
            ticket.setEta(demoEta(demo.etaOffset(), 0));
            ticket.setClosedAt(null);
            // The step is inferred again from the data, so it matches the status.
            ticket.setWorkflowStage(null);
            ticketRepository.save(ticket);
        }

        Ticket globex = demoTickets.get("demo-seed-globex");
        if (globex != null && !"Archived".equals(globex.getStatus())) {
            // The first request per seeded department is the seeded one; later
            // requests on this ticket were made by people and are left alone.
            Map<String, SmeRequest> seededByDepartment = new HashMap<>();
            smeRequestRepository.findByTicketId(globex.getId()).stream()
                    .sorted(Comparator.comparing(SmeRequest::getId))
                    .forEach(request -> seededByDepartment.putIfAbsent(request.getDepartment(), request));
            for (DemoSmeRequest demo : DEMO_SME_REQUESTS) {
                SmeRequest request = seededByDepartment.get(demo.department());
                if (request == null) continue;
                request.setStatus(demo.status());
                request.setEta(demoEta(demo.etaOffset(), demo.etaHour()));
                request.setSentAt(demoDate(-2).withHour(10));
                request.setReturnedAt(null);
                smeRequestRepository.save(request);
            }
        }
        System.out.println(">>> DataLoader: refreshed dates of " + demoTickets.size() + " seeded demo tickets.");
    }

    // The seeded tickets, by clientRef. A database seeded before tickets carried
    // a clientRef gets it now: the oldest unmarked ticket with the seed's
    // customer and business impact is the seeded one.
    private Map<String, Ticket> findDemoTickets() {
        List<Ticket> all = ticketRepository.findAll().stream()
                .sorted(Comparator.comparing(Ticket::getId))
                .toList();
        Map<String, Ticket> found = new HashMap<>();
        for (DemoTicket demo : DEMO_TICKETS) {
            all.stream()
                    .filter(t -> demo.clientRef().equals(t.getClientRef()))
                    .findFirst()
                    .or(() -> all.stream()
                            .filter(t -> t.getClientRef() == null
                                    && demo.customerName().equals(t.getCustomerName())
                                    && demo.businessImpact().equals(t.getBusinessImpact()))
                            .findFirst()
                            .map(t -> {
                                t.setClientRef(demo.clientRef());
                                return ticketRepository.save(t);
                            }))
                    .ifPresent(t -> found.put(demo.clientRef(), t));
        }
        return found;
    }

    private void seedFormQuestions(Long globexTicketId) {
        // InfoSec
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Do you have a SOC 2 Type II report?", "InfoSec", "Source Found", "Medium", "Q1"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Describe data encryption in transit.", "InfoSec", "Needs Review", "Medium", "Q2"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "What is your vulnerability disclosure policy?", "InfoSec", "Source Found", "Medium", "Q3"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Do you perform annual penetration testing?", "InfoSec", "Needs Review", "Medium", "Q4"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "What MFA mechanisms are supported?", "InfoSec", "Source Found", "Low", "Q5"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "How are privileged accounts managed?", "InfoSec", "SME Needed", "High", "Q6"));

        // Legal
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Do you have anti-bribery policies?", "Legal", "Source Found", "Medium", "Q7"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Where is customer data subject to jurisdiction?", "Legal", "SME Needed", "High", "Q8"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Do you have a data processing agreement template?", "Legal", "Needs Review", "Medium", "Q9"));

        // HR
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "What is your employee turnover rate?", "HR", "SME Needed", "Low", "Q10"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Do you conduct background checks on all staff?", "HR", "Needs Review", "Medium", "Q11"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "What security training do employees receive?", "HR", "Source Found", "Low", "Q12"));

        // Finance
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Are your financials audited by a third party?", "Finance", "Source Found", "Medium", "Q13"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Do you maintain cyber insurance?", "Finance", "SME Needed", "Medium", "Q14"));

        // ESG
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "What is your carbon neutrality target?", "ESG", "SME Needed", "Low", "Q15"));
        formQuestionRepository.save(new FormQuestion(globexTicketId,
                "Do you publish an annual sustainability report?", "ESG", "Needs Review", "Low", "Q16"));

        System.out.println(">>> DataLoader: inserted " + formQuestionRepository.count() + " form questions.");
    }

    private void seedData() {
        // ===== Company Overview =====
        repository.save(new KnowledgeBase("Company Overview", "Company background",
                "Cloudera is an enterprise data and AI platform company. It was founded in 2008 and is headquartered in Santa Clara, California. Cloudera provides a hybrid data platform for data management, analytics, and AI, and serves customers across financial services, telecommunications, healthcare, manufacturing and the public sector.",
                "Company Overview > Company background", d(2025, 9, 2), "Public", "General", null, true));

        repository.save(new KnowledgeBase("Company Overview", "Employees",
                "As of February 2023, Cloudera had approximately 3,000 employees worldwide, across offices in North America, Europe and Asia-Pacific. The largest functions by headcount are Engineering, Customer Success and Sales.",
                "Company Overview > Employees", d(2023, 2, 10), "Public", "HR", null, true));

        repository.save(new KnowledgeBase("Company Overview", "Ways of working",
                "Cloudera operates a hybrid working model. Most employees may work remotely for part of the week, with team-dependent expectations for in-office collaboration. Distributed and fully remote roles are supported where the role allows, subject to manager approval and local employment regulations.",
                "Company Overview > Ways of working", d(2025, 9, 2), "Public", "HR", null, true));

        repository.save(new KnowledgeBase("Company Overview", "Customer base",
                "Cloudera's customer base spans large enterprises and the Global 2000, with the strongest concentration in regulated industries such as banking, insurance, telecommunications and healthcare. Customers are located across North America, EMEA and Asia-Pacific.",
                "Company Overview > Customer base", d(2025, 9, 2), "Public", "General", null, true));

        repository.save(new KnowledgeBase("Company Overview", "Corporate activity",
                "Cloudera was taken private in 2021 and is privately held. There are no pending or planned mergers or acquisitions disclosed at the time of writing.",
                "Company Overview > Corporate activity", d(2025, 9, 2), "Customer-shareable", "Legal", null, true));

        // ===== Information Security Policy v3.2 =====
        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Encryption of data at rest",
                "Customer data at rest is encrypted using AES-256. Encryption keys are managed through a dedicated key management service (KMS) and are rotated on a regular basis. Backups are encrypted using the same standard.",
                "Information Security Policy v3.2 > Encryption of data at rest", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Encryption of data in transit",
                "Data in transit between clients and Cloudera services is encrypted using TLS version 1.2 or higher. Internal service-to-service communication within the production environment is also encrypted in transit.",
                "Information Security Policy v3.2 > Encryption of data in transit", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Access control",
                "Access to production systems and customer data follows the principle of least privilege. Authentication is supported through mechanisms such as Kerberos, LDAP and SAML-based single sign-on. Access is granted on a role basis, reviewed periodically, and protected by multi-factor authentication. Administrative access is logged.",
                "Information Security Policy v3.2 > Access control", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Disaster recovery and business continuity",
                "Cloudera maintains a documented disaster recovery and business continuity plan. The plan is reviewed and tested at least annually, and defines recovery time and recovery point objectives for critical services.",
                "Information Security Policy v3.2 > Disaster recovery and business continuity", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Core technology platform",
                "Cloudera's platform is built on a proven open-source foundation and runs across on-premises, public cloud and hybrid environments. Key components include data engineering, data warehousing, machine learning and operational database services, with security and governance applied consistently across workloads.",
                "Information Security Policy v3.2 > Core technology platform", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Incident response",
                "A documented incident response plan is maintained and reviewed annually. It covers detection, triage, escalation, containment and customer notification. Security incidents affecting customer data are communicated to affected customers in line with contractual and regulatory obligations.",
                "Information Security Policy v3.2 > Incident response", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Approach to AI",
                "Where AI features are used internally, Cloudera reviews each tool through its information security team before approval. Tools that send data outside Cloudera's controlled environment are generally not approved. Internal guidelines on responsible AI use and data bias mitigation are applied.",
                "Information Security Policy v3.2 > Approach to AI", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, false));

        // ===== Compliance and Certifications Summary =====
        repository.save(new KnowledgeBase("Compliance and Certifications Summary", "Certifications held",
                "Cloudera maintains SOC 2 Type II and ISO/IEC 27001 certification for its data platform, and operates in accordance with the EU General Data Protection Regulation (GDPR) and the California Consumer Privacy Act (CCPA). Cloudera also maintains a FedRAMP Moderate authorization for its government offering and supports PCI DSS.",
                "Compliance and Certifications Summary > Certifications held", d(2025, 9, 15), "Public", "Compliance", null, true));

        repository.save(new KnowledgeBase("Compliance and Certifications Summary", "Availability of reports",
                "The most recent SOC 2 Type II report is available to customers under a non-disclosure agreement on request. ISO 27001 certificates can be shared more broadly on request.",
                "Compliance and Certifications Summary > Availability of reports", d(2025, 9, 15), "NDA-required", "Compliance", null, true));

        repository.save(new KnowledgeBase("Compliance and Certifications Summary", "Upcoming audits and renewals",
                "The ISO 27001 surveillance audit and the annual SOC 2 Type II examination are conducted on a recurring annual cycle. The next surveillance activities are scheduled within the next twelve months.",
                "Compliance and Certifications Summary > Upcoming audits and renewals", d(2025, 9, 15), "Customer-shareable", "Compliance", null, true));

        repository.save(new KnowledgeBase("Compliance and Certifications Summary", "Penetration testing",
                "Third-party penetration testing is conducted at least annually. A summary of the most recent results is available to customers under a non-disclosure agreement.",
                "Compliance and Certifications Summary > Penetration testing", d(2025, 8, 20), "NDA-required", "InfoSec", null, true));

        // ===== Legal and Financial Overview =====
        repository.save(new KnowledgeBase("Legal and Financial Overview", "Litigation and regulatory matters",
                "Cloudera is not currently involved in any material litigation or regulatory action that would affect its ability to deliver services.",
                "Legal and Financial Overview > Litigation and regulatory matters", d(2025, 7, 12), "NDA-required", "Legal", null, true));

        repository.save(new KnowledgeBase("Legal and Financial Overview", "Financial position",
                "Cloudera reports more than one billion US dollars in annual recurring revenue. Detailed audited financial statements are private and can be provided to counterparties only under a non-disclosure agreement where appropriate.",
                "Legal and Financial Overview > Financial position", d(2025, 7, 12), "NDA-required", "Treasury", null, true));

        repository.save(new KnowledgeBase("Legal and Financial Overview", "Anti-bribery and anti-money-laundering",
                "Cloudera maintains an Anti-Bribery and Anti-Corruption policy that is reviewed periodically and acknowledged by employees. The company applies anti-money-laundering controls appropriate to its business.",
                "Legal and Financial Overview > Anti-bribery and anti-money-laundering", d(2025, 6, 30), "Customer-shareable", "Legal", null, true));

        repository.save(new KnowledgeBase("Legal and Financial Overview", "Insurance",
                "Cloudera holds cyber liability insurance. A certificate of insurance can be provided to customers under a non-disclosure agreement on request.",
                "Legal and Financial Overview > Insurance", d(2025, 7, 12), "NDA-required", "Treasury", null, true));

        // ===== HR and People Policy v2.1 =====
        repository.save(new KnowledgeBase("HR and People Policy v2.1", "Recruitment and hiring",
                "Cloudera follows a structured recruitment process for all roles. Candidates are assessed against documented role requirements, and hiring decisions involve more than one interviewer. Cloudera is an equal opportunity employer and does not discriminate on the basis of any protected characteristic.",
                "HR and People Policy v2.1 > Recruitment and hiring", d(2025, 10, 20), "Customer-shareable", "HR", null, true));

        repository.save(new KnowledgeBase("HR and People Policy v2.1", "Background checks",
                "Cloudera conducts background checks on new employees before their start date where permitted by local law. These checks include identity verification, right-to-work confirmation, and employment history verification. The scope of checks may be extended for roles with access to sensitive customer data.",
                "HR and People Policy v2.1 > Background checks", d(2025, 10, 20), "Customer-shareable", "HR", null, true));

        repository.save(new KnowledgeBase("HR and People Policy v2.1", "Credit checks",
                "Credit checks are carried out only for employees in finance, treasury, and senior leadership roles where there is a clear business justification. Credit checks are not performed for general staff, and candidate consent is obtained before any such check.",
                "HR and People Policy v2.1 > Credit checks", d(2025, 10, 20), "Customer-shareable", "HR", null, true));

        repository.save(new KnowledgeBase("HR and People Policy v2.1", "Training and awareness",
                "Employees complete mandatory security awareness training within their first month and on an annual basis thereafter. Role-specific training is provided where required, including data protection training for staff who handle personal data.",
                "HR and People Policy v2.1 > Training and awareness", d(2025, 10, 20), "Customer-shareable", "HR", null, true));

        repository.save(new KnowledgeBase("HR and People Policy v2.1", "Remote and hybrid work",
                "Cloudera supports hybrid and remote working subject to role suitability and manager approval. Remote workers must follow the same security policies as office-based staff, including the use of company-managed devices and approved network connections for accessing production systems.",
                "HR and People Policy v2.1 > Remote and hybrid work", d(2025, 10, 20), "Customer-shareable", "HR", null, true));

        repository.save(new KnowledgeBase("HR and People Policy v2.1", "Leavers and offboarding",
                "When an employee leaves, access to all systems and customer data is revoked on or before their final working day. Company equipment is returned, and a documented offboarding checklist is completed by the employee's manager and the IT team.",
                "HR and People Policy v2.1 > Leavers and offboarding", d(2025, 10, 20), "Customer-shareable", "HR", null, true));

        // ===== Finance and Tax Overview =====
        repository.save(new KnowledgeBase("Finance and Tax Overview", "Financial stability",
                "Cloudera is an established enterprise software company with more than one billion US dollars in annual recurring revenue. It funds its operations primarily through recurring subscription revenue.",
                "Finance and Tax Overview > Financial stability", d(2025, 7, 30), "Customer-shareable", "Treasury", null, true));

        repository.save(new KnowledgeBase("Finance and Tax Overview", "Audited financial statements",
                "As a privately held company, Cloudera's detailed financial statements are not public. Where contractually appropriate, relevant financial information can be shared with counterparties under a non-disclosure agreement.",
                "Finance and Tax Overview > Audited financial statements", d(2025, 7, 30), "NDA-required", "Treasury", null, true));

        repository.save(new KnowledgeBase("Finance and Tax Overview", "Tax compliance",
                "Cloudera is registered for tax in the jurisdictions where it has a taxable presence and files required tax returns on time. The company maintains a documented tax policy and is not aware of any material unresolved tax disputes.",
                "Finance and Tax Overview > Tax compliance", d(2025, 7, 30), "Customer-shareable", "Treasury", null, true));

        repository.save(new KnowledgeBase("Finance and Tax Overview", "Payment terms",
                "Standard customer payment terms are thirty days from the date of invoice. Alternative terms may be agreed contractually for enterprise customers. Cloudera accepts payment by the methods specified in the relevant order documentation.",
                "Finance and Tax Overview > Payment terms", d(2025, 7, 30), "Customer-shareable", "Treasury", null, true));

        repository.save(new KnowledgeBase("Finance and Tax Overview", "Financial controls",
                "Cloudera maintains segregation of duties for financial transactions, with defined approval limits for expenditure. Payments above a set threshold require dual authorisation. Financial controls are reviewed as part of the annual audit.",
                "Finance and Tax Overview > Financial controls", d(2025, 7, 30), "NDA-required", "Treasury", null, true));

        repository.save(new KnowledgeBase("Finance and Tax Overview", "VAT and invoicing",
                "Cloudera issues tax-compliant invoices where applicable and provides its tax registration details on request. Invoicing details, including legal entity and remittance information, are confirmed during contract onboarding.",
                "Finance and Tax Overview > VAT and invoicing", d(2025, 7, 30), "Customer-shareable", "Treasury", null, true));

        // ===== Unapproved chunk (demonstrates human review) =====
        repository.save(new KnowledgeBase("Finance and Tax Overview", "Carbon and ESG reporting",
                "Cloudera is preparing ESG and carbon-related disclosures. Targets and methodology are still being finalised and have not yet been approved for external sharing.",
                "Finance and Tax Overview > Carbon and ESG reporting", d(2026, 6, 5), "Customer-shareable", "ESG", null, false));

        System.out.println(">>> DataLoader: inserted " + repository.count() + " knowledge base chunks.");
    }
}
