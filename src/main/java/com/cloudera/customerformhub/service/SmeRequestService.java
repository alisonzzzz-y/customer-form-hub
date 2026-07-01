package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.SmeRequest;
import com.cloudera.customerformhub.repository.SmeRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SmeRequestService {

    private final SmeRequestRepository repository;

    public SmeRequestService(SmeRequestRepository repository) {
        this.repository = repository;
    }

    // Get all SME requests
    public List<SmeRequest> getAllSmeRequests() {
        return repository.findAll();
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

    // Delete
    public void deleteSmeRequest(Long id) {
        repository.deleteById(id);
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