package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.SmeRequestQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.SmeRequestQuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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

    // Delete one record
    public void delete(Long id) {
        repository.deleteById(id);
    }

    // Package all "SME Needed" questions of a ticket+department into an SME request.
    // Returns the list of created SmeRequestQuestion records.
    public List<SmeRequestQuestion> packageQuestions(Long smeRequestId, Long ticketId, String department) {
        // 1. Find this ticket's questions in this department that need SME input
        List<FormQuestion> deptQuestions =
                formQuestionRepository.findByTicketIdAndDepartment(ticketId, department);

        // 2. For each question that needs SME, create a link record
        return deptQuestions.stream()
                .filter(q -> "SME Needed".equals(q.getStatus()))
                .map(q -> {
                    SmeRequestQuestion link = new SmeRequestQuestion(
                            smeRequestId, q.getId(), "Pending",
                            "No source found in knowledge base", null);
                    return repository.save(link);
                })
                .toList();
    }
}