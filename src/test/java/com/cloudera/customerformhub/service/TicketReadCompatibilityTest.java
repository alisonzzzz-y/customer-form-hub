package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class TicketReadCompatibilityTest {

    @Autowired
    private TicketRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void unknownStoredStatusDoesNotBreakReadsOrGetRewritten() {
        Ticket dirty = new Ticket();
        dirty.setCustomerName("Historical test ticket");
        dirty.setStatus("Manual Legacy State");
        Long id = repository.saveAndFlush(dirty).getId();
        entityManager.clear();

        TicketService service = new TicketService(repository);

        assertThat(service.getAllTickets())
                .extracting(Ticket::getStatus)
                .contains("Manual Legacy State");
        assertThat(service.getTicketById(id).getStatus()).isEqualTo("Manual Legacy State");

        entityManager.clear();
        assertThat(repository.findById(id).orElseThrow().getStatus())
                .isEqualTo("Manual Legacy State");
    }

    @Test
    void unknownStatusIsStillRejectedOnWrite() {
        Ticket invalid = new Ticket();
        invalid.setCustomerName("Invalid write test");
        invalid.setStatus("Manual Legacy State");

        TicketService service = new TicketService(repository);

        assertThatThrownBy(() -> service.saveTicket(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ticket status");
    }
}
