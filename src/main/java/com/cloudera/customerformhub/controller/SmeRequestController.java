package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.entity.SmeRequest;
import com.cloudera.customerformhub.service.SmeRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sme-requests")
public class SmeRequestController {

    private final SmeRequestService smeRequestService;

    public SmeRequestController(SmeRequestService smeRequestService) {
        this.smeRequestService = smeRequestService;
    }

    // GET /api/sme-requests  → all SME requests
    @GetMapping
    public List<SmeRequest> getAllSmeRequests() {
        return smeRequestService.getAllSmeRequests();
    }

    // GET /api/sme-requests/ticket/{ticketId}  → all SME requests for one ticket
    @GetMapping("/ticket/{ticketId}")
    public List<SmeRequest> getSmeRequestsByTicket(@PathVariable Long ticketId) {
        return smeRequestService.getSmeRequestsByTicket(ticketId);
    }

    // GET /api/sme-requests/{id}  → one SME request
    @GetMapping("/{id}")
    public ResponseEntity<SmeRequest> getSmeRequestById(@PathVariable Long id) {
        SmeRequest request = smeRequestService.getSmeRequestById(id);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(request);
    }

    // POST /api/sme-requests  → create
    @PostMapping
    public SmeRequest createSmeRequest(@RequestBody SmeRequest smeRequest) {
        return smeRequestService.saveSmeRequest(smeRequest);
    }

    // POST /api/sme-requests/dispatch/ticket/{ticketId}  → create or reuse department SME requests
    @PostMapping("/dispatch/ticket/{ticketId}")
    public List<SmeRequest> dispatchForTicket(@PathVariable Long ticketId) {
        return smeRequestService.dispatchForTicket(ticketId);
    }

    // PUT /api/sme-requests/{id}  → update (e.g. record ETA, mark returned)
    @PutMapping("/{id}")
    public ResponseEntity<SmeRequest> updateSmeRequest(@PathVariable Long id, @RequestBody SmeRequest smeRequest) {
        SmeRequest updated = smeRequestService.updateSmeRequest(id, smeRequest);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/sme-requests/{id}  → delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSmeRequest(@PathVariable Long id) {
        SmeRequest existing = smeRequestService.getSmeRequestById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        smeRequestService.deleteSmeRequest(id);
        return ResponseEntity.noContent().build();
    }
}