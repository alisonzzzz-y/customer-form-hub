package com.cloudera.customerformhub.config;

import com.cloudera.customerformhub.entity.KnowledgeBase;
import com.cloudera.customerformhub.repository.KnowledgeBaseRepository;
import com.cloudera.customerformhub.service.EmbeddingService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final KnowledgeBaseRepository repository;
    private final EmbeddingService embeddingService;

    public DataLoader(KnowledgeBaseRepository repository,
                      EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    private LocalDateTime d(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay();
    }

    @Override
    public void run(String... args) {
        // Seed the 20 chunks if the table is empty
        if (repository.count() == 0) {
            seedData();
        }

        // Generate embeddings for chunks that don't have one yet
        embeddingService.generateEmbeddingsForAll();
    }

    private void seedData() {
        // the order of parameters
        repository.save(new KnowledgeBase("Company Overview", "Company background",
                "Northwind Data Ltd. is a data platform company providing enterprise data management and analytics software. It was founded in 2008 and is incorporated in Ireland, with its registered office in Dublin. It serves customers across financial services, healthcare, telecommunications and the public sector.",
                "Company Overview > Company background", d(2025, 9, 2), "Public", "General", null, true));

        repository.save(new KnowledgeBase("Company Overview", "Employees",
                "As of February 2023, Northwind Data Ltd. had approximately 3,000 employees worldwide, across offices in Europe, North America and Asia-Pacific. The largest functions by headcount are Engineering, Customer Success and Sales.",
                "Company Overview > Employees", d(2023, 2, 10), "Public", "HR", null, true));

        repository.save(new KnowledgeBase("Company Overview", "Ways of working",
                "Northwind operates a hybrid working model. Most employees may work remotely for part of the week, with team-dependent expectations for in-office collaboration. Distributed and fully remote roles are supported where the role allows, subject to manager approval and local employment regulations.",
                "Company Overview > Ways of working", d(2025, 9, 2), "Public", "HR", null, true));

        repository.save(new KnowledgeBase("Company Overview", "Customer base",
                "Northwind's customer base spans large enterprises and mid-market organisations, with the strongest concentration in regulated industries such as banking, insurance and healthcare. Customers are located primarily in the EMEA and North America regions.",
                "Company Overview > Customer base", d(2025, 9, 2), "Public", "General", null, true));

        repository.save(new KnowledgeBase("Company Overview", "Corporate activity",
                "There are no pending or planned mergers or acquisitions at the time of writing. The company is privately held and is not currently engaged in external fundraising activities.",
                "Company Overview > Corporate activity", d(2025, 9, 2), "Customer-shareable", "Legal", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Encryption of data at rest",
                "All customer data at rest is encrypted using AES-256. Encryption keys are managed through a dedicated key management service and are rotated on an annual basis. Backups are encrypted using the same standard.",
                "Information Security Policy v3.2 > Encryption of data at rest", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Encryption of data in transit",
                "All data in transit between clients and Northwind services is encrypted using TLS version 1.2 or higher. Internal service-to-service communication within the production environment is also encrypted in transit.",
                "Information Security Policy v3.2 > Encryption of data in transit", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Access control",
                "Access to production systems and customer data follows the principle of least privilege. Access is granted on a role basis, reviewed quarterly, and protected by multi-factor authentication. All administrative access is logged.",
                "Information Security Policy v3.2 > Access control", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Disaster recovery and business continuity",
                "Northwind maintains a documented disaster recovery and business continuity plan. The plan is reviewed and tested at least annually, and defines recovery time and recovery point objectives for critical services.",
                "Information Security Policy v3.2 > Disaster recovery and business continuity", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Core technology platform",
                "Northwind's core platform is built primarily in Java and Python, running on a major public cloud provider. Key dependencies include managed database services, container orchestration, and a small number of third-party APIs subject to vendor security review.",
                "Information Security Policy v3.2 > Core technology platform", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Incident response",
                "A documented incident response plan is maintained and reviewed annually. It covers detection, triage, escalation, containment and customer notification. Security incidents affecting customer data are communicated to affected customers in line with contractual and regulatory obligations.",
                "Information Security Policy v3.2 > Incident response", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Information Security Policy v3.2", "Approach to AI",
                "Where AI features are used internally, Northwind reviews each tool through its information security team before approval. Tools that send data outside Northwind's controlled environment are generally not approved. Internal guidelines on responsible AI use and data bias mitigation are applied.",
                "Information Security Policy v3.2 > Approach to AI", d(2025, 11, 3), "Customer-shareable", "InfoSec", null, false));

        repository.save(new KnowledgeBase("Compliance and Certifications Summary", "Certifications held",
                "Northwind Data Ltd. holds ISO 27001 certification for its information security management system and maintains an annual SOC 2 Type II report. The company operates in accordance with the EU General Data Protection Regulation (GDPR).",
                "Compliance and Certifications Summary > Certifications held", d(2025, 9, 15), "Public", "Compliance", null, true));

        repository.save(new KnowledgeBase("Compliance and Certifications Summary", "Availability of reports",
                "The most recent SOC 2 Type II report is available to customers under a non-disclosure agreement on request. ISO 27001 certificates can be shared publicly.",
                "Compliance and Certifications Summary > Availability of reports", d(2025, 9, 15), "NDA-required", "Compliance", null, true));

        repository.save(new KnowledgeBase("Compliance and Certifications Summary", "Upcoming audits and renewals",
                "The next ISO 27001 surveillance audit is scheduled within the next twelve months. The annual SOC 2 Type II examination is ongoing and renews each year.",
                "Compliance and Certifications Summary > Upcoming audits and renewals", d(2025, 9, 15), "Customer-shareable", "Compliance", null, true));

        repository.save(new KnowledgeBase("Compliance and Certifications Summary", "Penetration testing",
                "Third-party penetration testing is conducted at least annually. A summary of the most recent results is available to customers under a non-disclosure agreement.",
                "Compliance and Certifications Summary > Penetration testing", d(2025, 8, 20), "NDA-required", "InfoSec", null, true));

        repository.save(new KnowledgeBase("Legal and Financial Overview", "Litigation and regulatory matters",
                "Northwind Data Ltd. has not been involved in any material litigation, regulatory action, or compliance investigation in the past five years.",
                "Legal and Financial Overview > Litigation and regulatory matters", d(2025, 7, 12), "NDA-required", "Legal", null, true));

        repository.save(new KnowledgeBase("Legal and Financial Overview", "Financial position",
                "Audited financial statements are prepared annually and can be provided to counterparties under a non-disclosure agreement. There are no outstanding debts, liens, or encumbrances on the company's assets that would be considered material.",
                "Legal and Financial Overview > Financial position", d(2025, 7, 12), "NDA-required", "Treasury", null, true));

        repository.save(new KnowledgeBase("Legal and Financial Overview", "Anti-bribery and anti-money-laundering",
                "Northwind maintains an Anti-Bribery and Anti-Corruption policy that is reviewed annually and acknowledged by all employees. The company applies anti-money-laundering controls appropriate to its business.",
                "Legal and Financial Overview > Anti-bribery and anti-money-laundering", d(2025, 6, 30), "Customer-shareable", "Legal", null, true));

        repository.save(new KnowledgeBase("Legal and Financial Overview", "Insurance",
                "Northwind holds cyber liability insurance. A certificate of insurance can be provided to customers under a non-disclosure agreement on request.",
                "Legal and Financial Overview > Insurance", d(2025, 7, 12), "NDA-required", "Treasury", null, true));

        System.out.println(">>> DataLoader: inserted " + repository.count() + " knowledge base chunks.");
    }
}