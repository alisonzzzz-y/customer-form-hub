package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;

import com.cloudera.customerformhub.entity.SmeRequest;
import com.cloudera.customerformhub.repository.SmeRequestRepository;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SmeRequestService {

    private final SmeRequestRepository repository;
    private final FormQuestionRepository questionRepository;
    private final SmeRequestQuestionService smeRequestQuestionService;

    public SmeRequestService(SmeRequestRepository repository,
                             FormQuestionRepository questionRepository,
                             SmeRequestQuestionService smeRequestQuestionService) {
        this.repository = repository;
        this.questionRepository = questionRepository;
        this.smeRequestQuestionService = smeRequestQuestionService;
    }

    // Get all SME requests, with overdue auto-detection applied
    public List<SmeRequest> getAllSmeRequests() {
        List<SmeRequest> requests = repository.findAll();
        requests.forEach(this::applyOverdueStatus);
        return requests;
    }

    // Get all SME requests for one ticket, with overdue auto-detection applied
    public List<SmeRequest> getSmeRequestsByTicket(Long ticketId) {
        List<SmeRequest> requests = repository.findByTicketId(ticketId);
        requests.forEach(this::applyOverdueStatus);
        return requests;
    }

    // Get one SME request by id
    public SmeRequest getSmeRequestById(Long id) {
        SmeRequest request = repository.findById(id).orElse(null);
        if (request != null) applyOverdueStatus(request);
        return request;
    }

    // Create or update
    public SmeRequest saveSmeRequest(SmeRequest smeRequest) {
        return repository.save(smeRequest);
    }

    // Dispatch all SME Needed questions for one ticket into department-level SME
    // requests. A question already waiting in an open request stays there; new
    // ones go into the department's unsent draft, or a new request if the
    // existing one was already sent.
    public List<SmeRequest> dispatchForTicket(Long ticketId) {
        List<FormQuestion> questions = questionRepository.findByTicketIdAndRetiredAtIsNull(ticketId);
        List<SmeRequest> existingRequests = repository.findByTicketId(ticketId);

        Map<String, Integer> newQuestionCounts = new LinkedHashMap<>();
        Set<String> departments = new LinkedHashSet<>();
        for (FormQuestion question : questions) {
            if ("SME Needed".equals(question.getStatus()) && question.getDepartment() != null) {
                departments.add(question.getDepartment());
                if (!smeRequestQuestionService.isInOpenRequest(question.getId())) {
                    newQuestionCounts.merge(question.getDepartment(), 1, Integer::sum);
                }
            }
        }

        List<SmeRequest> dispatchedRequests = new ArrayList<>();
        for (String department : departments) {
            List<SmeRequest> forDepartment = existingRequests.stream()
                    .filter(r -> department.equals(r.getDepartment()))
                    .toList();
            int newCount = newQuestionCounts.getOrDefault(department, 0);
            if (newCount == 0) {
                // Everything is already packaged; report the open request.
                forDepartment.stream().filter(r -> !"Returned".equals(r.getStatus()))
                        .findFirst().ifPresent(dispatchedRequests::add);
                continue;
            }
            SmeRequest request = forDepartment.stream()
                    .filter(smeRequestQuestionService::acceptsNewQuestions)
                    .findFirst()
                    .orElse(null);
            if (request == null) {
                request = repository.save(new SmeRequest(
                        ticketId, department, department + " Team", newCount,
                        null, null, null, null, null));
            }
            smeRequestQuestionService.packageQuestions(request.getId(), ticketId, department);
            dispatchedRequests.add(request);
        }
        return dispatchedRequests;
    }

    // Partial update: only non-null fields are copied onto the existing row
    public SmeRequest updateSmeRequest(Long id, SmeRequest changes) {
        SmeRequest existing = repository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        if (changes.getDepartment() != null) existing.setDepartment(changes.getDepartment());
        if (changes.getTeamName() != null) existing.setTeamName(changes.getTeamName());
        if (changes.getQuestionCount() != null) existing.setQuestionCount(changes.getQuestionCount());
        if (changes.getEta() != null) existing.setEta(changes.getEta());
        if (changes.getStatus() != null) existing.setStatus(changes.getStatus());
        if (changes.getConfirmedBy() != null) existing.setConfirmedBy(changes.getConfirmedBy());
        if (changes.getReturnedAt() != null) existing.setReturnedAt(changes.getReturnedAt());
        // sentAt is recorded once, when a user confirms a prepared draft was sent;
        // ticketId and an existing sentAt are intentionally never overwritten
        if (changes.getSentAt() != null && existing.getSentAt() == null) existing.setSentAt(changes.getSentAt());
        return repository.save(existing);
    }

    // Undo a "Returned" mark: clears the returnedAt timestamp and rolls the
    // status back. PUT cannot express this — under our partial-update
    // semantics a null field means "don't change" — so the intent gets its
    // own endpoint.
    public SmeRequest unreturn(Long id) {
        SmeRequest existing = repository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        existing.setReturnedAt(null);
        existing.setStatus(existing.getEta() != null ? "ETA Confirmed" : "Waiting for ETA");
        return repository.save(existing);
    }

    // If the ETA has passed and it hasn't been returned yet, mark it Overdue
    private void applyOverdueStatus(SmeRequest request) {
        boolean notReturned = request.getReturnedAt() == null
                && !"Returned".equals(request.getStatus());
        boolean etaPassed = request.getEta() != null
                && request.getEta().isBefore(LocalDateTime.now());
        if (notReturned && etaPassed) {
            request.setStatus("Overdue");
        }
    }
}