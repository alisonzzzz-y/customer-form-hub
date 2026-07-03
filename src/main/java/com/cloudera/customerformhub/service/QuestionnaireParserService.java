package com.cloudera.customerformhub.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionnaireParserService {

    // A parsed question row: section + question text
    public record ParsedQuestion(String section, String questionText) {}

    // Parse an uploaded .xlsx file that follows the standard template:
    // row 1 = headers, with a "Section" column and a "Question" column.
    public List<ParsedQuestion> parse(MultipartFile file) {
        List<ParsedQuestion> questions = new ArrayList<>();

        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = workbook.getSheetAt(0);   // use the first sheet
            if (sheet == null) {
                return questions;
            }

            // 1. Read the header row to find which column is Section and which is Question
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                return questions;
            }

            int sectionCol = -1;
            int questionCol = -1;
            for (Cell cell : header) {
                String name = getCellString(cell).trim().toLowerCase();
                if (name.equals("section")) {
                    sectionCol = cell.getColumnIndex();
                } else if (name.equals("question")) {
                    questionCol = cell.getColumnIndex();
                }
            }

            // If there's no Question column, we can't parse
            if (questionCol == -1) {
                throw new IllegalArgumentException(
                        "Template must have a 'Question' column in the first row.");
            }

            // 2. Read each data row (skip the header)
            int firstDataRow = sheet.getFirstRowNum() + 1;
            for (int r = firstDataRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String questionText = getCellString(row.getCell(questionCol)).trim();
                if (questionText.isEmpty()) continue;   // skip blank rows

                String section = "";
                if (sectionCol != -1) {
                    section = getCellString(row.getCell(sectionCol)).trim();
                }

                questions.add(new ParsedQuestion(section, questionText));
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read the uploaded Excel file", e);
        }

        return questions;
    }

    // Safely read any cell as a String (handles text, numbers, blanks, nulls)
    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}