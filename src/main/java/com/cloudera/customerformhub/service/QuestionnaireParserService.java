package com.cloudera.customerformhub.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionnaireParserService {

    // A parsed question row: section + question text
    public record ParsedQuestion(String section, String questionText) {}

    // Parse all sheets in an uploaded .xlsx file that follows the standard template.
    public List<ParsedQuestion> parse(MultipartFile file) {
        List<SheetQuestions> contributingSheets = new ArrayList<>();
        boolean questionColumnFound = false;

        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<ParsedQuestion> sheetQuestions = parseSheet(sheet, "");

                if (hasQuestionColumn(sheet)) {
                    questionColumnFound = true;
                }

                if (!sheetQuestions.isEmpty()) {
                    contributingSheets.add(
                            new SheetQuestions(sheet.getSheetName(), sheetQuestions)
                    );
                }
            }

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The uploaded Excel workbook could not be read.",
                    ex
            );
        }

        if (!questionColumnFound) {
            throw new IllegalArgumentException(
                    "No 'Question' column found in any sheet: please use the questionnaire template");
        }

        List<ParsedQuestion> questions = new ArrayList<>();
        boolean prefixSections = contributingSheets.size() > 1;

        for (SheetQuestions sheetQuestions : contributingSheets) {
            for (ParsedQuestion question : sheetQuestions.questions()) {
                if (!prefixSections) {
                    questions.add(question);
                    continue;
                }

                String section = question.section().isBlank()
                        ? sheetQuestions.sheetName()
                        : sheetQuestions.sheetName() + " / " + question.section();

                questions.add(
                        new ParsedQuestion(section, question.questionText())
                );
            }
        }

        return questions;
    }

    // Parse one worksheet using the existing header and row rules.
    private List<ParsedQuestion> parseSheet(Sheet sheet, String sectionPrefix) {
        List<ParsedQuestion> questions = new ArrayList<>();

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

        // If there's no Question column, skip this sheet
        if (questionCol == -1) {
            return questions;
        }

        // 2. Read each data row (skip the header)
        int firstDataRow = sheet.getFirstRowNum() + 1;

        for (int r = firstDataRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String questionText =
                    getCellString(row.getCell(questionCol)).trim();

            if (questionText.isEmpty()) continue;   // skip blank rows

            String section = "";

            if (sectionCol != -1) {
                section = getCellString(row.getCell(sectionCol)).trim();
            }

            if (!sectionPrefix.isBlank()) {
                section = section.isBlank()
                        ? sectionPrefix
                        : sectionPrefix + " / " + section;
            }

            questions.add(
                    new ParsedQuestion(section, questionText)
            );
        }

        return questions;
    }

    // Check whether a worksheet contains a Question column in its header row.
    private boolean hasQuestionColumn(Sheet sheet) {
        Row header = sheet.getRow(sheet.getFirstRowNum());

        if (header == null) {
            return false;
        }

        for (Cell cell : header) {
            String name = getCellString(cell).trim().toLowerCase();

            if (name.equals("question")) {
                return true;
            }
        }

        return false;
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

    private record SheetQuestions(
            String sheetName,
            List<ParsedQuestion> questions
    ) {}
}
