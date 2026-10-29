package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.service.SmeAnswerImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/sme-request-questions")
public class SmeAnswerImportController {
    private final SmeAnswerImportService service;

    public SmeAnswerImportController(SmeAnswerImportService service) {
        this.service = service;
    }

    @PostMapping("/request/{requestId}/import-preview")
    public Map<Long, String> preview(@PathVariable Long requestId, @RequestParam("file") MultipartFile file) {
        return service.preview(requestId, file);
    }
}
