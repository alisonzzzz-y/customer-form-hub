package com.cloudera.customerformhub.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketServiceStatusTest {

    @Test
    void acceptsTheFullFrontendLifecycleStatus() {
        assertEquals("Ready for Review", TicketService.normaliseStatus("Ready for Review"));
        assertEquals("Approved", TicketService.normaliseStatus("Approved"));
        assertEquals("Sent", TicketService.normaliseStatus("Sent"));
        assertEquals("Closed", TicketService.normaliseStatus("Closed"));
    }

    @Test
    void migratesLegacyCompletedToClosed() {
        assertEquals("Intake Review", TicketService.normaliseStatus("Intake Missing"));
        assertEquals("In Progress", TicketService.normaliseStatus("In Review"));
        assertEquals("Closed", TicketService.normaliseStatus("Completed"));
    }

    @Test
    void rejectsUnknownStatuses() {
        assertThrows(IllegalArgumentException.class,
                () -> TicketService.normaliseStatus("Almost Finished"));
    }
}
