package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.SmeRequest;
import com.cloudera.customerformhub.entity.SmeRequestQuestion;
import com.cloudera.customerformhub.entity.Ticket;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.SmeRequestQuestionRepository;
import com.cloudera.customerformhub.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailComposeService {

    private final SmeRequestQuestionRepository smeRequestQuestionRepository;
    private final FormQuestionRepository formQuestionRepository;
    private final TicketRepository ticketRepository;

    public EmailComposeService(SmeRequestQuestionRepository smeRequestQuestionRepository,
                               FormQuestionRepository formQuestionRepository,
                               TicketRepository ticketRepository) {
        this.smeRequestQuestionRepository = smeRequestQuestionRepository;
        this.formQuestionRepository = formQuestionRepository;
        this.ticketRepository = ticketRepository;
    }

    // Compose plain-text email content for one SME request.
    public Map<String, String> composeSmeEmail(SmeRequest request) {
        Ticket ticket = ticketRepository.findById(request.getTicketId()).orElse(null);
        String customerName = resolveCustomerName(ticket, request.getTicketId());
        String teamName = resolveTeamName(request);

        List<String> questions = getQuestionTexts(request.getId());

        String subject = "[Action needed] SME input for "
                + customerName
                + " questionnaire – "
                + request.getDepartment()
                + " ("
                + questions.size()
                + " questions)";

        StringBuilder body = new StringBuilder();

        body.append("Hi ").append(teamName).append(",\n\n");

        if (questions.isEmpty()) {
            body.append("(No questions are currently linked to this request.)\n\n");
        }

        body.append("The GOM team is completing a customer questionnaire for ")
                .append(customerName)
                .append(" and needs your department's input on the questions below.\n\n");

        if (ticket != null && ticket.getDeadline() != null) {
            body.append("Customer deadline: ")
                    .append(ticket.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .append(" (UTC)\n\n");
        }

        for (int i = 0; i < questions.size(); i++) {
            body.append(i + 1)
                    .append(". ")
                    .append(questions.get(i))
                    .append("\n");
        }

        if (!questions.isEmpty()) {
            body.append("\n");
        }

        body.append("Please reply with a realistic ETA for returning answers so we can track this request.\n\n");
        body.append("Thanks,\n");
        body.append("Global Order Management");

        Map<String, String> result = new LinkedHashMap<>();
        result.put("to", "");
        result.put("subject", subject);
        result.put("body", body.toString());

        return result;
    }

    // Resolve customer name, falling back to the ticket id if the ticket is missing.
    private String resolveCustomerName(Ticket ticket, Long ticketId) {
        if (ticket != null && ticket.getCustomerName() != null && !ticket.getCustomerName().isBlank()) {
            return ticket.getCustomerName();
        }
        return "ticket " + ticketId;
    }

    // Resolve the recipient team display name.
    private String resolveTeamName(SmeRequest request) {
        if (request.getTeamName() != null && !request.getTeamName().isBlank()) {
            return request.getTeamName();
        }
        return request.getDepartment() + " Team";
    }

    // Resolve linked SME request questions into original question text.
    private List<String> getQuestionTexts(Long smeRequestId) {
        List<SmeRequestQuestion> links = smeRequestQuestionRepository.findBySmeRequestId(smeRequestId);
        List<String> questions = new ArrayList<>();

        for (SmeRequestQuestion link : links) {
            FormQuestion question = formQuestionRepository.findById(link.getQuestionId()).orElse(null);
            if (question != null && question.getQuestionText() != null && !question.getQuestionText().isBlank()) {
                questions.add(question.getQuestionText());
            }
        }

        return questions;
    }
}