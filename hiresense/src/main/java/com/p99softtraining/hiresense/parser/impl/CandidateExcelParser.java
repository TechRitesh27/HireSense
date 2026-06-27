package com.p99softtraining.hiresense.parser.impl;

import com.p99softtraining.hiresense.dto.request.CreateCandidateRequest;
import com.p99softtraining.hiresense.parser.CandidateFileParser;
import com.p99softtraining.hiresense.parser.ParsedCandidateData;
import com.p99softtraining.hiresense.parser.ParsedRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Component
public class CandidateExcelParser implements CandidateFileParser {

    private static final Map<String, List<String>> FIELD_MAPPINGS = Map.of(
            "fullName", List.of("full name", "candidate name", "name", ""),
            "email", List.of("email", "email address", "mail"),
            "phone", List.of("phone", "mobile", "phone number", "contact"),
            "collegeName", List.of("college", "college name", "university"),
            "degree", List.of("degree", "qualification"),
            "branch", List.of("branch", "department", "specialization"),
            "graduationYear", List.of("graduation year", "passout year", "year"),
            "resumeUrl", List.of("resume", "resume url", "cv link")
    );

    @Override
    public boolean supports(String contentType, String fileName) {
        if (contentType != null) {
            return contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    || contentType.equals("application/vnd.ms-excel");
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            return lower.endsWith(".xlsx") || lower.endsWith(".xls");
        }
        return false;
    }

    @Override
    public ParsedCandidateData parse(MultipartFile file) throws IOException {
        try (java.io.InputStream inputStream = file.getInputStream()) {
            return parse(inputStream);
        }
    }

    @Override
    public ParsedCandidateData parse(java.io.InputStream inputStream) throws IOException {
        List<ParsedRow> parsedRows = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return ParsedCandidateData.builder()
                        .totalRows(0)
                        .rows(Collections.emptyList())
                        .missingColumns(Collections.emptyList())
                        .build();
            }

            Map<String, Integer> excelHeaders = new HashMap<>();
            for (Cell cell : headerRow) {
                String header = normalize(cell.getStringCellValue());
                excelHeaders.put(header, cell.getColumnIndex());
            }

            Map<String, Integer> resolvedColumns = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : FIELD_MAPPINGS.entrySet()) {
                String dtoField = entry.getKey();
                for (String alias : entry.getValue()) {
                    Integer index = excelHeaders.get(normalize(alias));
                    if (index != null) {
                        resolvedColumns.put(dtoField, index);
                        break;
                    }
                }
            }

            List<String> requiredFields = List.of("fullName", "email", "phone");
            for (String field : requiredFields) {
                if (!resolvedColumns.containsKey(field)) {
                    missingFields.add(field);
                }
            }

            if (!missingFields.isEmpty()) {
                return ParsedCandidateData.builder()
                        .totalRows(0)
                        .rows(Collections.emptyList())
                        .missingColumns(missingFields)
                        .build();
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                try {
                    CreateCandidateRequest request = new CreateCandidateRequest();
                    
                    request.setFullName(getCellValue(getCellByFieldName(row, resolvedColumns, "fullName")));
                    request.setEmail(getCellValue(getCellByFieldName(row, resolvedColumns, "email")));
                    request.setPhone(getCellValue(getCellByFieldName(row, resolvedColumns, "phone")));
                    request.setCollegeName(getCellValue(getCellByFieldName(row, resolvedColumns, "collegeName")));
                    request.setDegree(getCellValue(getCellByFieldName(row, resolvedColumns, "degree")));
                    request.setBranch(getCellValue(getCellByFieldName(row, resolvedColumns, "branch")));

                    String graduationYear = getCellValue(getCellByFieldName(row, resolvedColumns, "graduationYear"));
                    request.setGraduationYear(graduationYear.isBlank() ? null : Integer.parseInt(graduationYear));

                    request.setResumeUrl(getCellValue(getCellByFieldName(row, resolvedColumns, "resumeUrl")));

                    parsedRows.add(new ParsedRow(i + 1, request, null));
                } catch (Exception e) {
                    parsedRows.add(new ParsedRow(i + 1, null, e.getMessage()));
                }
            }

            return ParsedCandidateData.builder()
                    .totalRows(lastRowNum)
                    .rows(parsedRows)
                    .missingColumns(missingFields)
                    .build();
        }
    }

    private Cell getCellByFieldName(Row row, Map<String, Integer> resolvedColumns, String fieldName) {
        Integer index = resolvedColumns.get(fieldName);
        if (index == null) {
            return null;
        }
        return row.getCell(index);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private String normalize(String value) {
        return value.trim()
                .toLowerCase()
                .replace("_", " ")
                .replace("-", " ");
    }
}
