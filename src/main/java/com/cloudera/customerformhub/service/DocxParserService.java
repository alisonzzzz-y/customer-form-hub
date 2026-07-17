package com.cloudera.customerformhub.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocxParserService {

    // Extract raw text from a Word document, including paragraphs and table cells.
    public String extractText(MultipartFile file) {
        StringBuilder text = new StringBuilder();

        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendIfNotBlank(text, paragraph.getText());
            }

            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        appendIfNotBlank(text, cell.getText());
                    }
                }
            }
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The uploaded Word document could not be read.",
                    ex
            );
        }

        return text.toString();
    }

    // Append one text piece if it contains useful content.
    private void appendIfNotBlank(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(value.trim()).append("\n");
        }
    }
}
