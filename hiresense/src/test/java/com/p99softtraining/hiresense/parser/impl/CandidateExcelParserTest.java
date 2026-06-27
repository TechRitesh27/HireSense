package com.p99softtraining.hiresense.parser.impl;

import com.p99softtraining.hiresense.dto.request.CreateCandidateRequest;
import com.p99softtraining.hiresense.parser.ParsedCandidateData;
import com.p99softtraining.hiresense.parser.ParsedRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CandidateExcelParserTest {

    private final CandidateExcelParser parser = new CandidateExcelParser();

    @Test
    void testSupports() {
        assertTrue(parser.supports("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", null));
        assertTrue(parser.supports(null, "candidates.xlsx"));
        assertFalse(parser.supports("text/csv", "candidates.csv"));
    }

    @Test
    void testParseValidExcel() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Full Name");
            header.createCell(1).setCellValue("Email Address");
            header.createCell(2).setCellValue("Phone Number");
            header.createCell(3).setCellValue("College");
            header.createCell(4).setCellValue("Degree");
            header.createCell(5).setCellValue("Branch");
            header.createCell(6).setCellValue("Graduation Year");
            header.createCell(7).setCellValue("Resume URL");

            // Row 1
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("John Doe");
            row1.createCell(1).setCellValue("john.doe@example.com");
            row1.createCell(2).setCellValue("1234567890");
            row1.createCell(3).setCellValue("MIT");
            row1.createCell(4).setCellValue("B.Tech");
            row1.createCell(5).setCellValue("CSE");
            row1.createCell(6).setCellValue(2024);
            row1.createCell(7).setCellValue("https://resume.com/john");

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray())) {
                ParsedCandidateData data = parser.parse(bis);

                assertEquals(1, data.getTotalRows());
                assertTrue(data.getMissingColumns().isEmpty());
                assertEquals(1, data.getRows().size());

                ParsedRow parsedRow = data.getRows().get(0);
                assertEquals(2, parsedRow.getRowNumber());
                assertNull(parsedRow.getErrorMessage());

                CreateCandidateRequest request = parsedRow.getRequest();
                assertNotNull(request);
                assertEquals("John Doe", request.getFullName());
                assertEquals("john.doe@example.com", request.getEmail());
                assertEquals("1234567890", request.getPhone());
                assertEquals("MIT", request.getCollegeName());
                assertEquals("B.Tech", request.getDegree());
                assertEquals("CSE", request.getBranch());
                assertEquals(2024, request.getGraduationYear());
                assertEquals("https://resume.com/john", request.getResumeUrl());
            }
        }
    }

    @Test
    void testParseMissingRequiredColumns() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Full Name");
            // Email and Phone are missing!

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray())) {
                ParsedCandidateData data = parser.parse(bis);

                assertEquals(0, data.getTotalRows());
                assertTrue(data.getRows().isEmpty());
                assertTrue(data.getMissingColumns().contains("email"));
                assertTrue(data.getMissingColumns().contains("phone"));
            }
        }
    }
}
