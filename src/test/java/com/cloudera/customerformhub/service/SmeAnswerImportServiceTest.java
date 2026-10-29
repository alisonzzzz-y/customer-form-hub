package com.cloudera.customerformhub.service;

import com.cloudera.customerformhub.entity.FormQuestion;
import com.cloudera.customerformhub.entity.SmeRequestQuestion;
import com.cloudera.customerformhub.repository.FormQuestionRepository;
import com.cloudera.customerformhub.repository.SmeRequestQuestionRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmeAnswerImportServiceTest {
    final SmeRequestQuestionRepository links = mock(SmeRequestQuestionRepository.class);
    final FormQuestionRepository questions = mock(FormQuestionRepository.class);
    final SmeAnswerImportService service = new SmeAnswerImportService(links, questions);

    SmeAnswerImportServiceTest() {
        var a = new FormQuestion(); a.setId(1L); a.setQuestionText("First question?");
        var b = new FormQuestion(); b.setId(2L); b.setQuestionText("Second question?");
        var l1 = new SmeRequestQuestion(); l1.setQuestionId(1L);
        var l2 = new SmeRequestQuestion(); l2.setQuestionId(2L);
        when(links.findBySmeRequestId(5L)).thenReturn(List.of(l1, l2));
        when(questions.findAllById(List.of(1L, 2L))).thenReturn(List.of(a, b));
    }

    MockMultipartFile workbook(String[][] rows) throws Exception {
        try (var book = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = book.createSheet("Answers");
            for (int r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) row.createCell(c).setCellValue(rows[r][c]);
            }
            book.write(out);
            return new MockMultipartFile("file", "returned.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test void readsRealAnswersAndMatchesReorderedRowsWithoutSaving() throws Exception {
        var result = service.preview(5L, workbook(new String[][] {
                {"#", "Question", "SME Answer"},
                {"2", "Second question?", "真实答案, with punctuation\nand a second line"},
                {"1", "First question?", "Actual first answer"}
        }));
        assertEquals("Actual first answer", result.get(1L));
        assertEquals("真实答案, with punctuation\nand a second line", result.get(2L));
        verify(links, never()).save(any());
        verify(questions, never()).save(any());
    }

    @Test void allowsPartialReturns() throws Exception {
        var result = service.preview(5L, workbook(new String[][] {
                {"Question", "Answer"}, {"First question?", ""}, {"Second question?", "Real partial answer"}
        }));
        assertEquals(1, result.size());
        assertTrue(result.containsKey(2L));
    }

    @Test void rejectsInvalidWorkbook() {
        assertThrows(IllegalArgumentException.class, () -> service.preview(5L,
                new MockMultipartFile("file", "fake.xlsx", "", "not Excel".getBytes())));
    }

    @Test void rejectsMissingColumns() throws Exception {
        var file = workbook(new String[][] {{"Question", "Notes"}, {"First question?", "answer"}});
        assertThrows(IllegalArgumentException.class, () -> service.preview(5L, file));
    }

    @Test void rejectsUnknownQuestion() throws Exception {
        var file = workbook(new String[][] {{"Question", "Answer"}, {"Another request's question?", "answer"}});
        assertThrows(IllegalArgumentException.class, () -> service.preview(5L, file));
    }

    @Test void rejectsDuplicateAnswers() throws Exception {
        var file = workbook(new String[][] {{"Question", "Answer"}, {"First question?", "one"}, {"First question?", "two"}});
        assertThrows(IllegalArgumentException.class, () -> service.preview(5L, file));
    }

    @Test void rejectsEmptyAnswers() throws Exception {
        var file = workbook(new String[][] {{"Question", "Answer"}, {"First question?", ""}});
        assertThrows(IllegalArgumentException.class, () -> service.preview(5L, file));
    }
}
