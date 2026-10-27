package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.TicketRepository;
import com.cloudera.customerformhub.service.FormQuestionService.ImportResult;
import com.cloudera.customerformhub.service.QuestionnaireParserService.ParsedQuestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Function;

// Imports a customer form into a ticket and records which file it was.
// The same file (same bytes) is a repeat submission: nothing is inserted and
// nothing is retired. A different file is a new version of the form: it is
// always parsed and reconciled, so its content replaces the old version
// without overwriting reviewed rows.
@Service
public class QuestionnaireImportService {

    public static final String CREATED = "CREATED";
    public static final String UNCHANGED = "UNCHANGED";
    public static final String UPDATED = "UPDATED";

    private final FormQuestionService formQuestionService;
    private final TicketRepository ticketRepository;

    public QuestionnaireImportService(FormQuestionService formQuestionService,
                                      TicketRepository ticketRepository) {
        this.formQuestionService = formQuestionService;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public List<FormQuestion> importFile(Ticket ticket, String fileName, byte[] bytes,
                                         List<ParsedQuestion> parsed,
                                         Function<List<String>, List<String>> classifier) {
        String hash = sha256(bytes);
        boolean firstImport = ticket.getFormFileHash() == null
                && formQuestionService.getQuestionsByTicket(ticket.getId()).isEmpty();
        boolean sameFile = hash.equals(ticket.getFormFileHash());

        ImportResult result = formQuestionService.importParsedQuestions(
                ticket.getId(), parsed, classifier, !sameFile && !firstImport);

        ticket.setFormFileName(fileName);
        ticket.setFormFileHash(hash);
        ticket.setFormImportedAt(LocalDateTime.now());
        ticket.setFormImportOutcome(firstImport ? CREATED : sameFile ? UNCHANGED : UPDATED);
        ticket.setFormQuestionsAdded(result.added());
        ticket.setFormQuestionsRetired(result.retired());
        ticketRepository.save(ticket);
        return result.questions();
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
