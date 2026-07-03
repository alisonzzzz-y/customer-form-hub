package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.QuestionWithAnswer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    private final FinalReviewService finalReviewService;

    public ExcelExportService(FinalReviewService finalReviewService) {
        this.finalReviewService = finalReviewService;
    }

    // Build an .xlsx file (as bytes) containing all questions and their final answers for a ticket
    public byte[] exportTicketAnswers(Long ticketId) {
        List<QuestionWithAnswer> rows = finalReviewService.getReviewForTicket(ticketId);

        // try-with-resources: the workbook and stream are closed automatically
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Answers");

            // --- Header row ---
            String[] headers = {"#", "Question", "Department", "Answer", "Status", "Edited", "Approved By"};
            Row headerRow = sheet.createRow(0);

            // Bold style for the header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Data rows ---
            int rowNum = 1;
            for (QuestionWithAnswer row : rows) {
                Row excelRow = sheet.createRow(rowNum);

                excelRow.createCell(0).setCellValue(rowNum);                       // #
                excelRow.createCell(1).setCellValue(safe(row.getQuestionText()));  // Question
                excelRow.createCell(2).setCellValue(safe(row.getDepartment()));    // Department
                excelRow.createCell(3).setCellValue(
                        row.isAnswered() ? safe(row.getAnswerText()) : "(No answer yet)"); // Answer
                excelRow.createCell(4).setCellValue(safe(row.getQuestionStatus())); // Status
                excelRow.createCell(5).setCellValue(
                        Boolean.TRUE.equals(row.getIsEdited()) ? "Yes" : "No");     // Edited
                excelRow.createCell(6).setCellValue(safe(row.getApprovedBy()));    // Approved By

                rowNum++;
            }

            // Auto-size columns so the content fits
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write the workbook to a byte array
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    // Helper: turn null into empty string so setCellValue never gets null
    private String safe(String value) {
        return value == null ? "" : value;
    }
}