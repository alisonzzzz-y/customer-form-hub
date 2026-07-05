package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.dto.QuestionWithAnswer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private final FinalReviewService finalReviewService;

    public ExcelExportService(FinalReviewService finalReviewService) {
        this.finalReviewService = finalReviewService;
    }

    // Build an .xlsx file (as bytes) containing all questions and their final answers for a ticket.
    // Draft answers are NOT exported as text: human review is mandatory before anything
    // customer-facing, so only Confirmed answers appear in full.
    public byte[] exportTicketAnswers(Long ticketId) {
        List<QuestionWithAnswer> rows = finalReviewService.getReviewForTicket(ticketId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Answers");

            // --- Header row ---
            String[] headers = {"#", "Question", "Department", "Answer",
                    "Status", "Edited", "Approved By", "Last Updated (UTC)"};
            Row headerRow = sheet.createRow(0);

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

                excelRow.createCell(0).setCellValue(rowNum);
                excelRow.createCell(1).setCellValue(safe(row.getQuestionText()));
                excelRow.createCell(2).setCellValue(safe(row.getDepartment()));
                excelRow.createCell(3).setCellValue(answerCell(row));
                excelRow.createCell(4).setCellValue(safe(row.getQuestionStatus()));
                excelRow.createCell(5).setCellValue(
                        Boolean.TRUE.equals(row.getIsEdited()) ? "Yes" : "No");
                excelRow.createCell(6).setCellValue(safe(row.getApprovedBy()));
                excelRow.createCell(7).setCellValue(
                        row.getAnswerUpdatedAt() != null
                                ? row.getAnswerUpdatedAt().format(TIMESTAMP) : "");

                rowNum++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    // Decide what goes in the Answer column:
    // no answer → placeholder; Draft → placeholder (never export unreviewed text); Confirmed → the text
    private String answerCell(QuestionWithAnswer row) {
        if (!row.isAnswered()) {
            return "(No answer yet)";
        }
        if (!"Confirmed".equals(row.getApprovalStatus())) {
            return "(Draft – not confirmed)";
        }
        return safe(row.getAnswerText());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}