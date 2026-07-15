package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.SmeRequestQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.SmeRequestQuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SmeRequestQuestionService {

    private final SmeRequestQuestionRepository repository;
    private final FormQuestionRepository formQuestionRepository;

    public SmeRequestQuestionService(SmeRequestQuestionRepository repository,
                                     FormQuestionRepository formQuestionRepository) {
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
    public List<SmeRequestQuestion> packageQuestions(Long smeRequestId, Long ticketId, String department) {
        // 1. Which questions are already in this SME request?
        Set<Long> alreadyLinked = repository.findBySmeRequestId(smeRequestId).stream()
                .map(SmeRequestQuestion::getQuestionId)
                .collect(Collectors.toSet());

        // 2. Find this ticket's questions in this department that need SME input
        List<FormQuestion> deptQuestions =
                formQuestionRepository.findByTicketIdAndDepartment(ticketId, department);

        // 3. Create a link record only for questions not linked yet
        return deptQuestions.stream()
                .filter(q -> "SME Needed".equals(q.getStatus()))
                .filter(q -> !alreadyLinked.contains(q.getId()))
                .map(q -> {
                    SmeRequestQuestion link = new SmeRequestQuestion(
                            smeRequestId, q.getId(), "Pending",
                            "No source found in knowledge base", null);
                    return repository.save(link);
                })
                .toList();
    }
}