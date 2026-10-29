package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.SmeRequestQuestionRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmeAnswerImportService {
    private final SmeRequestQuestionRepository links;
    private final FormQuestionRepository questions;

    public SmeAnswerImportService(SmeRequestQuestionRepository links, FormQuestionRepository questions) {
        this.links = links;
        this.questions = questions;
    }

    // Preview only. The reviewer must explicitly save the imported answers.
    public Map<Long, String> preview(Long requestId, MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null ||
                !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Upload a non-empty .xlsx workbook.");
        }
        var ids = links.findBySmeRequestId(requestId).stream().map(l -> l.getQuestionId()).toList();
        var byText = questions.findAllById(ids).stream()
                .filter(q -> q.getRetiredAt() == null)
                .collect(Collectors.groupingBy(q -> normalise(q.getQuestionText())));
        Map<Long, String> answers = new LinkedHashMap<>();
        boolean foundHeaders = false;
        try (var input = file.getInputStream(); Workbook book = WorkbookFactory.create(input)) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            for (Sheet sheet : book) {
                int questionCol = -1, answerCol = -1;
                for (Row row : sheet) {
                    if (questionCol < 0 || answerCol < 0) {
                        int qc = -1, ac = -1;
                        for (Cell cell : row) {
                            String header = normalise(formatter.formatCellValue(cell));
                            if (header.equals("question")) qc = cell.getColumnIndex();
                            if (header.equals("sme answer") || header.equals("answer")) ac = cell.getColumnIndex();
                        }
                        if (qc >= 0 && ac >= 0) {
                            questionCol = qc;
                            answerCol = ac;
                            foundHeaders = true;
                        }
                        continue;
                    }
                    Cell answerCell = row.getCell(answerCol);
                    if (answerCell != null && answerCell.getCellType() == CellType.FORMULA) {
                        throw new IllegalArgumentException("Use plain answer values instead of formulas.");
                    }
                    String answer = formatter.formatCellValue(answerCell).trim();
                    if (answer.isEmpty()) continue;
                    String text = normalise(formatter.formatCellValue(row.getCell(questionCol)));
                    List<FormQuestion> matches = byText.getOrDefault(text, List.of());
                    if (matches.size() != 1) {
                        throw new IllegalArgumentException("An answered row does not uniquely match a question in this SME request. Keep the original Question column.");
                    }
                    if (answers.putIfAbsent(matches.get(0).getId(), answer) != null) {
                        throw new IllegalArgumentException("The workbook contains duplicate answers for the same question.");
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("The file could not be read as an Excel workbook. Upload a valid .xlsx file.");
        }
        if (!foundHeaders) throw new IllegalArgumentException("Include a Question column and an SME Answer or Answer column.");
        if (answers.isEmpty()) throw new IllegalArgumentException("No non-empty answers were found in the workbook.");
        return answers;
    }

    private static String normalise(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
