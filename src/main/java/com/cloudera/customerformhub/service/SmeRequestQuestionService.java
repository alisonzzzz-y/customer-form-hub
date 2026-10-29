package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.SmeRequest;
import com.cloudera.customerformhub.entity.SmeRequestQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.SmeRequestQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cloudera.customerformhub.repository.SmeRequestRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SmeRequestQuestionService {

    private final SmeRequestQuestionRepository repository;
    private final FormQuestionRepository formQuestionRepository;

    private final SmeRequestRepository requests;

    public SmeRequestQuestionService(SmeRequestQuestionRepository repository,
                                     FormQuestionRepository formQuestionRepository, SmeRequestRepository requests) {
        this.requests = requests;
        this.repository = repository;
        this.formQuestionRepository = formQuestionRepository;
    }

    // Get all questions in a given SME request
    public List<SmeRequestQuestion> getByRequest(Long smeRequestId) {
        return repository.findBySmeRequestId(smeRequestId);
    }

    // Get the SME-request records for a given question
    public List<SmeRequestQuestion> getByQuestion(Long questionId) {
        return repository.findByQuestionId(questionId);
    }

    // Get one by id (null if not found)
    public SmeRequestQuestion getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    // Save (create or update) one record
    public SmeRequestQuestion save(SmeRequestQuestion item) {
        return repository.save(item);
    }

    // Record the SME's returned answer for one question, marking it Returned
    public SmeRequestQuestion recordAnswer(Long id, String returnedAnswer) {
        SmeRequestQuestion item = repository.findById(id).orElse(null);
        if (item == null) {
            return null;
        }
        item.setReturnedAnswer(returnedAnswer);
        item.setStatus("Returned");
        return repository.save(item);
    }

    // Package all "SME Needed" questions of a ticket+department into an SME request.
    // Idempotent: questions already linked to this request are skipped, so calling
    // this twice (e.g. a double-click) can never create duplicates.
    @Transactional
    public List<SmeRequestQuestion> packageQuestions(Long smeRequestId, Long ticketId, String department) {
        var request = requests.findForPackaging(smeRequestId).orElseThrow(
                () -> new IllegalArgumentException("SME request not found"));
        if (!ticketId.equals(request.getTicketId()) || !department.equals(request.getDepartment())) {
            throw new IllegalArgumentException("The package must belong to this request's ticket and department");
        }
        if ("Returned".equals(request.getStatus())) {
            throw new IllegalArgumentException("Create a new request for questions added after a completed request");
        }
        // 1. Which questions are already in this SME request?
        List<SmeRequestQuestion> existingLinks = repository.findBySmeRequestId(smeRequestId);
        Set<Long> alreadyLinked = existingLinks.stream()
                .map(SmeRequestQuestion::getQuestionId)
                .collect(Collectors.toSet());

        // 2. This ticket's questions in this department that need SME input and
        //    are not already waiting in another open request.
        List<FormQuestion> toAdd = formQuestionRepository
                .findByTicketIdAndDepartmentAndRetiredAtIsNull(ticketId, department).stream()
                .filter(q -> "SME Needed".equals(q.getStatus()))
                .filter(q -> !alreadyLinked.contains(q.getId()))
                .filter(q -> !inOtherOpenRequest(q.getId(), smeRequestId))
                .toList();

        // 3. A request that was already sent keeps its send record and ETA:
        //    questions that come up later go into a new request instead.
        if (!toAdd.isEmpty() && !existingLinks.isEmpty() && wasSent(request)) {
            throw new IllegalArgumentException("This SME request was already sent. Put the new questions "
                    + "in a new request so the SME's ETA and send record are kept.");
        }

        for (FormQuestion q : toAdd) {
            repository.save(new SmeRequestQuestion(
                    smeRequestId, q.getId(), "Pending",
                    "No source found in knowledge base", null));
        }
        var all = repository.findBySmeRequestId(smeRequestId);
        request.setQuestionCount(all.size());
        if (!toAdd.isEmpty()) {
            // The draft changed, so it needs a fresh send confirmation.
            request.setStatus(SmeRequest.DRAFT_PREPARED);
            request.setSentAt(null);
        }
        requests.save(request);
        return all;
    }

    // Whether new questions can still be added to this request: it is not
    // returned, and it either has no questions yet or is an unsent draft.
    public boolean acceptsNewQuestions(SmeRequest request) {
        if ("Returned".equals(request.getStatus())) return false;
        return repository.findBySmeRequestId(request.getId()).isEmpty() || !wasSent(request);
    }

    private static boolean wasSent(SmeRequest request) {
        return request.getSentAt() != null || !SmeRequest.DRAFT_PREPARED.equals(request.getStatus());
    }

    // Whether the question is already linked to a request that is not returned.
    public boolean isInOpenRequest(Long questionId) {
        return inOtherOpenRequest(questionId, null);
    }

    private boolean inOtherOpenRequest(Long questionId, Long smeRequestId) {
        return repository.findByQuestionId(questionId).stream()
                .filter(link -> smeRequestId == null || !link.getSmeRequestId().equals(smeRequestId))
                .map(link -> requests.findById(link.getSmeRequestId()).orElse(null))
                .anyMatch(other -> other != null && !"Returned".equals(other.getStatus()));
    }
}
