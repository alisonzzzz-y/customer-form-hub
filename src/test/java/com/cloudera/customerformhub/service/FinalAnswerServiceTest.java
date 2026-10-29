package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.*;
import com.cloudera.customerformhub.repository.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FinalAnswerServiceTest {
    final FinalAnswerRepository answers = mock(FinalAnswerRepository.class);
    final FormQuestionRepository questions = mock(FormQuestionRepository.class);
    final KnowledgeBaseRepository knowledge = mock(KnowledgeBaseRepository.class);
    final TicketRepository tickets = mock(TicketRepository.class);
    final FinalAnswerService service = new FinalAnswerService(answers, questions, knowledge, tickets);
    final Ticket ticket = new Ticket();

    FinalAnswerServiceTest() {
        var q = new FormQuestion(); q.setId(1L); q.setTicketId(2L);
        var source = new KnowledgeBase(); source.setSharingStatus("NDA-required");
        when(questions.findById(1L)).thenReturn(Optional.of(q));
        when(tickets.findById(2L)).thenReturn(Optional.of(ticket));
        when(knowledge.findById(3L)).thenReturn(Optional.of(source));
        when(answers.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    FinalAnswer input(boolean edited) {
        var a = new FinalAnswer();
        a.setQuestionId(1L); a.setSourceChunkId(3L); a.setSourceType("Knowledge Base");
        a.setApprovalStatus("Confirmed"); a.setAnswerText("Restricted response");
        a.setIsEdited(edited);
        return a;
    }
    @Test void rejectsBothDirectAndEditedKnowledgeWithoutNda() {
        ticket.setNdaStatus("No");
        assertThrows(IllegalArgumentException.class, () -> service.saveAnswer(input(false)));
        assertThrows(IllegalArgumentException.class, () -> service.saveAnswer(input(true)));
        verify(answers, never()).save(any());
        verify(questions, never()).save(any());
    }
    @Test void allowsEditedKnowledgeWhenNdaIsInPlace() {
        ticket.setNdaStatus("Yes");
        assertEquals("Confirmed", service.saveAnswer(input(true)).getApprovalStatus());
    }
    @Test void smeReplacementClearsOldKnowledgeAttribution() {
        var old = input(false);
        when(answers.findByQuestionId(1L)).thenReturn(List.of(old));
        var sme = new FinalAnswer();
        sme.setQuestionId(1L); sme.setSourceType("SME"); sme.setAnswerText("SME response");
        sme.setApprovalStatus("Confirmed");
        var result = service.saveAnswer(sme);
        assertNull(result.getSourceChunkId());
        assertEquals("SME", result.getSourceType());
        assertNull(questions.findById(1L).orElseThrow().getReviewOutcome());
    }
}
