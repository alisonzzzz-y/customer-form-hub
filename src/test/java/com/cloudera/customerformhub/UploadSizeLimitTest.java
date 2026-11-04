package com.cloudera.customerformhub;

import com.cloudera.customerformhub.config.DataLoader;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Upload limits are enforced by the real servlet container, not by MockMvc,
 * so these checks run against an embedded server.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadSizeLimitTest {

    private static final int MB = 1024 * 1024;

    @Autowired
    private TestRestTemplate rest;

    @MockitoBean
    private DataLoader dataLoader;

    @Test
    void parsesAWorkbookOverTheOldOneMegabyteLimit() throws Exception {
        byte[] workbook = paddedWorkbook();
        assertThat(workbook.length).isGreaterThan(2 * MB);

        ResponseEntity<String> response = upload(workbook);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Do you encrypt data at rest?");
    }

    @Test
    void rejectsAFileOverTheLimitWithAClearMessage() {
        ResponseEntity<String> response = upload(new byte[12 * MB]);

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody()).contains("larger than the 10 MB upload limit");
    }

    private ResponseEntity<String> upload(byte[] bytes) {
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "form.xlsx";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.postForEntity("/api/questionnaire/parse", new HttpEntity<>(form, headers), String.class);
    }

    // One real question, plus random text on another sheet so the file stays large once zipped.
    private byte[] paddedWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet questions = workbook.createSheet("InfoSec");
            questions.createRow(0).createCell(0).setCellValue("Question");
            questions.createRow(1).createCell(0).setCellValue("Do you encrypt data at rest?");

            Sheet padding = workbook.createSheet("Attachments");
            Random random = new Random(1);
            byte[] chunk = new byte[24_000];
            for (int i = 0; i < 100; i++) {
                random.nextBytes(chunk);
                Row row = padding.createRow(i);
                row.createCell(0).setCellValue(Base64.getEncoder().encodeToString(chunk));
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
