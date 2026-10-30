package com.cloudera.customerformhub.config;

import com.cloudera.customerformhub.entity.SmeRequest;
import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.KnowledgeBaseRepository;
import com.cloudera.customerformhub.repository.RetrievalEvaluationRunRepository;
import com.cloudera.customerformhub.repository.SmeRequestRepository;
import com.cloudera.customerformhub.repository.TicketRepository;
import com.cloudera.customerformhub.service.EmbeddingService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The rolling demo refresh must only reset the seeded demo rows.
class DataLoaderRefreshTest {

    private final TicketRepository tickets = mock(TicketRepository.class);
    private final SmeRequestRepository requests = mock(SmeRequestRepository.class);
    private final DataLoader loader = new DataLoader(
            mock(KnowledgeBaseRepository.class), mock(EmbeddingService.class), tickets, requests,
            mock(FormQuestionRepository.class), mock(RetrievalEvaluationRunRepository.class), false, true);

    private static Ticket ticket(long id, String customer, String impact, String status) {
        Ticket t = new Ticket(customer, "—", "Sarah Chen", status, "High", "Yes",
                LocalDateTime.of(2026, 1, 15, 0, 0), impact, null, LocalDateTime.of(2026, 1, 1, 0, 0));
        t.setId(id);
        return t;
    }

    private static SmeRequest request(long id, long ticketId, String department, String status) {
        SmeRequest r = new SmeRequest(ticketId, department, department + " Team", 1,
                null, status, null, null, null);
        r.setId(id);
        return r;
    }

    @Test
    void resetsOnlySeededDemoRowsAndLeavesPeoplesTicketsAlone() {
        // Seeded before tickets carried a clientRef, then overwritten by the old refresh.
        Ticket globex = ticket(2, "Globex Inc", "Renewal, medium value", "Closed");
        globex.setWorkflowStage("final");
        Ticket archivedDemo = ticket(1, "Acme Corp", "New logo, evaluating platform", "Archived");
        archivedDemo.setClientRef("demo-seed-acme");
        Ticket mine = ticket(70, "Vandelay Industries", "Renewal", "In Progress");
        mine.setClientRef("tk-user-70");
        mine.setWorkflowStage("review");
        // Same customer and impact as a seed, but newer: a person made it.
        Ticket lookalike = ticket(71, "Globex Inc", "Renewal, medium value", "In Progress");
        when(tickets.findAll()).thenReturn(List.of(mine, globex, archivedDemo, lookalike));
        when(tickets.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SmeRequest seeded = request(1, 2, "InfoSec", "Returned");
        SmeRequest added = request(9, 2, "InfoSec", "Draft Prepared");
        when(requests.findByTicketId(2L)).thenReturn(List.of(added, seeded));

        loader.refreshDemoData();

        assertThat(globex.getClientRef()).isEqualTo("demo-seed-globex");
        assertThat(globex.getStatus()).isEqualTo("Intake Review");
        assertThat(globex.getAssignedTo()).isEqualTo("Sarah");
        assertThat(globex.getWorkflowStage()).isNull();
        assertThat(seeded.getStatus()).isEqualTo("ETA Confirmed");
        assertThat(seeded.getEta()).isNotNull();

        assertThat(archivedDemo.getStatus()).isEqualTo("Archived");
        assertThat(mine.getStatus()).isEqualTo("In Progress");
        assertThat(mine.getWorkflowStage()).isEqualTo("review");
        assertThat(mine.getDeadline()).isEqualTo(LocalDateTime.of(2026, 1, 15, 0, 0));
        assertThat(lookalike.getClientRef()).isNull();
        assertThat(lookalike.getStatus()).isEqualTo("In Progress");
        assertThat(added.getStatus()).isEqualTo("Draft Prepared");
        verify(tickets, never()).save(mine);
        verify(tickets, never()).save(archivedDemo);
        verify(tickets, never()).save(lookalike);
        verify(requests, never()).save(added);
    }
}
