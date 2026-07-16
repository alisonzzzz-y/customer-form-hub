package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.entity.SmeRequest;
import com.cloudera.customerformhub.service.EmailComposeService;
import com.cloudera.customerformhub.service.SmeRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sme-requests")
public class SmeRequestController {

    private final SmeRequestService smeRequestService;
    private final EmailComposeService emailComposeService;

    public SmeRequestController(SmeRequestService smeRequestService,
                                EmailComposeService emailComposeService) {
        this.smeRequestService = smeRequestService;
        this.emailComposeService = emailComposeService;
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

    // GET /api/sme-requests/{id}/email  → render email content for one SME request
    @GetMapping("/{id}/email")
    public ResponseEntity<Map<String, String>> composeSmeEmail(@PathVariable Long id) {
        SmeRequest request = smeRequestService.getSmeRequestById(id);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(emailComposeService.composeSmeEmail(request));
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

    // POST /api/sme-requests/{id}/unreturn  → undo the "Returned" mark
    @PostMapping("/{id}/unreturn")
    public ResponseEntity<SmeRequest> unreturn(@PathVariable Long id) {
        SmeRequest updated = smeRequestService.unreturn(id);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }
}