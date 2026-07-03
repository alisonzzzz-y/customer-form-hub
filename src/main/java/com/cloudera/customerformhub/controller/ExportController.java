package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExcelExportService excelExportService;

    public ExportController(ExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    // GET /api/export/ticket/{ticketId}  → download an .xlsx of all answers for the ticket
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<byte[]> exportTicket(@PathVariable Long ticketId) {
        byte[] excelBytes = excelExportService.exportTicketAnswers(ticketId);

        String filename = "ticket-" + ticketId + "-answers.xlsx";

        return ResponseEntity.ok()
                // tell the browser this is a file to download, with this filename
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                // tell the browser this is an Excel file
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}