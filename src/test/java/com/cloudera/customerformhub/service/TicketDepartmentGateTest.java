package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketDepartmentGateTest {
    @Test
    void reviewRequiresAssignedDepartmentsAndAllowsCustomTeams() {
        var tickets = mock(TicketRepository.class);
        var questions = mock(FormQuestionRepository.class);
        var service = new TicketService(tickets, questions);
        var ticket = new Ticket();
        ticket.setWorkflowStage("grouping");
        when(tickets.findById(1L)).thenReturn(Optional.of(ticket));
        var patch = new Ticket();
        patch.setWorkflowStage("review");
        when(questions.findByTicketIdAndRetiredAtIsNull(1L)).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> service.updateTicket(1L, patch));
        var question = new FormQuestion();
        when(questions.findByTicketIdAndRetiredAtIsNull(1L)).thenReturn(List.of(question));
        for (String department : new String[] { null, "", " ", "General", " tbd " }) {
            question.setDepartment(department);
            assertThrows(IllegalArgumentException.class, () -> service.updateTicket(1L, patch));
            assertEquals("grouping", ticket.getWorkflowStage());
        }
        verify(tickets, never()).save(any());
        question.setDepartment("Customer Operations");
        service.updateTicket(1L, patch);
        assertEquals("review", ticket.getWorkflowStage());
        verify(tickets).save(ticket);
    }
}
