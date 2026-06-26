package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.dto.request.CreateCandidateRequest;
import com.p99softtraining.hiresense.dto.response.CandidateResponse;
import com.p99softtraining.hiresense.dto.response.ExcelUploadResponse;
import com.p99softtraining.hiresense.dto.response.RowErrorResponse;
import com.p99softtraining.hiresense.entity.Candidate;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.entity.User;
import com.p99softtraining.hiresense.enums.CandidateStatus;
import com.p99softtraining.hiresense.exception.ResourceAlreadyExistsException;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.CandidateRepository;
import com.p99softtraining.hiresense.repository.HiringDriveRepository;
import com.p99softtraining.hiresense.repository.UserRepository;
import com.p99softtraining.hiresense.service.CandidateService;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.p99softtraining.hiresense.mapper.CandidatesExcelMapper.FIELD_MAPPINGS;

@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final HiringDriveRepository hiringDriveRepository;
    private final UserRepository userRepository;

    @Override
    public CandidateResponse createCandidate(
            UUID hiringDriveId,
            CreateCandidateRequest request
    ) {

        HiringDrive hiringDrive = getCompanyHiringDrive(hiringDriveId);

        if (candidateRepository.existsByHiringDriveIdAndEmail(
                hiringDrive.getId(),
                request.getEmail()
        )) {
            throw new ResourceAlreadyExistsException("Candidate email already exists in this hiring drive");
        }

        Candidate candidate = new Candidate();
        candidate.setHiringDrive(hiringDrive);
        candidate.setFullName(request.getFullName());
        candidate.setEmail(request.getEmail());
        candidate.setPhone(request.getPhone());
        candidate.setCollegeName(request.getCollegeName());
        candidate.setDegree(request.getDegree());
        candidate.setBranch(request.getBranch());
        candidate.setGraduationYear(request.getGraduationYear());
        candidate.setResumeUrl(request.getResumeUrl());
        candidate.setStatus(CandidateStatus.IMPORTED);

        return mapCandidate(candidateRepository.save(candidate));
    }

    @Override
    public List<CandidateResponse> getCandidatesForHiringDrive(UUID hiringDriveId) {

        HiringDrive hiringDrive = getCompanyHiringDrive(hiringDriveId);

        return candidateRepository.findByHiringDriveIdOrderByCreatedAtDesc(hiringDrive.getId())
                .stream()
                .map(this::mapCandidate)
                .toList();
    }

    private HiringDrive getCompanyHiringDrive(UUID hiringDriveId) {

        Company company = getCurrentUserCompany();

        HiringDrive hiringDrive = hiringDriveRepository.findById(hiringDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hiring drive not found"));

        if (!hiringDrive.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("Hiring drive not found");
        }

        return hiringDrive;
    }

    private Company getCurrentUserCompany() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        if (user.getCompany() == null) {
            throw new IllegalArgumentException("Current user is not assigned to a company");
        }

        return user.getCompany();
    }

    private CandidateResponse mapCandidate(Candidate candidate) {

        HiringDrive hiringDrive = candidate.getHiringDrive();

        return CandidateResponse.builder()
                .id(candidate.getId())
                .hiringDriveId(hiringDrive.getId())
                .hiringDriveTitle(hiringDrive.getTitle())
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .collegeName(candidate.getCollegeName())
                .degree(candidate.getDegree())
                .branch(candidate.getBranch())
                .graduationYear(candidate.getGraduationYear())
                .resumeUrl(candidate.getResumeUrl())
                .status(candidate.getStatus())
                .build();
    }


    @Override
    public ExcelUploadResponse uploadCandidatesFromExcel(
            UUID hiringDriveId,
            MultipartFile file
    ) {

        int successCount = 0;
        int failedCount = 0;

        List<RowErrorResponse> errors = new ArrayList<>();

        List<String> missingFields = new ArrayList<>();

        try (
                Workbook workbook =
                        new XSSFWorkbook(file.getInputStream())
        ) {

            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);

            Map<String, Integer> excelHeaders =
                    new HashMap<>();

            for (Cell cell : headerRow) {

                String header = normalize(
                        cell.getStringCellValue()
                );

                excelHeaders.put(
                        header,
                        cell.getColumnIndex()
                );
            }

            Map<String, Integer> resolvedColumns =
                    new HashMap<>();

            for (Map.Entry<String, List<String>> entry
                    : FIELD_MAPPINGS.entrySet()) {

                String dtoField = entry.getKey();

                for (String alias : entry.getValue()) {

                    Integer index =
                            excelHeaders.get(normalize(alias));

                    if (index != null) {

                        resolvedColumns.put(
                                dtoField,
                                index
                        );

                        break;
                    }
                }
            }

            List<String> requiredFields = List.of(
                    "fullName",
                    "email",
                    "phone"
            );

            for (String field : requiredFields) {

                if (!resolvedColumns.containsKey(field)) {

                    missingFields.add(field);
                }
            }

            if (!missingFields.isEmpty()) {

                return ExcelUploadResponse.builder()
                        .totalRows(0)
                        .successCount(0)
                        .failedCount(0)
                        .missingColumns(missingFields)
                        .errors(List.of())
                        .build();
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                try {

                    CreateCandidateRequest request =
                            new CreateCandidateRequest();

                    request.setFullName(
                            getCellValue(
                                    row.getCell(
                                            resolvedColumns.get("fullName")
                                    )
                            )
                    );

                    request.setEmail(
                            getCellValue(
                                    row.getCell(
                                            resolvedColumns.get("email")
                                    )
                            )
                    );

                    request.setPhone(
                            getCellValue(
                                    row.getCell(
                                            resolvedColumns.get("phone")
                                    )
                            )
                    );

                    request.setCollegeName(
                            getCellValue(
                                    row.getCell(
                                            resolvedColumns.get("collegeName")
                                    )
                            )
                    );

                    request.setDegree(
                            getCellValue(
                                    row.getCell(
                                            resolvedColumns.get("degree")
                                    )
                            )
                    );

                    request.setBranch(
                            getCellValue(
                                    row.getCell(
                                            resolvedColumns.get("branch")
                                    )
                            )
                    );

                    String graduationYear =
                            getCellValue(
                                    row.getCell(
                                            resolvedColumns.get("graduationYear")
                                    )
                            );

                    request.setGraduationYear(
                            graduationYear.isBlank()
                                    ? null
                                    : Integer.parseInt(graduationYear)
                    );

                    request.setResumeUrl(
                            getCellValue(
                                    row.getCell(
                                            resolvedColumns.get("resumeUrl")
                                    )
                            )
                    );

                    createCandidate(hiringDriveId, request);

                    successCount++;

                } catch (Exception e) {

                    failedCount++;

                    errors.add(
                            new RowErrorResponse(
                                    i + 1,
                                    e.getMessage()
                            )
                    );
                }
            }

            return ExcelUploadResponse.builder()
                    .totalRows(sheet.getLastRowNum())
                    .successCount(successCount)
                    .failedCount(failedCount)
                    .missingColumns(missingFields)
                    .errors(errors)
                    .build();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to process Excel file",
                    e
            );
        }
    }

    private String getCellValue(Cell cell) {

        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {

            case STRING ->
                    cell.getStringCellValue().trim();

            case NUMERIC -> {

                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }

                yield String.valueOf(
                        (long) cell.getNumericCellValue()
                );
            }

            case BOOLEAN ->
                    String.valueOf(
                            cell.getBooleanCellValue()
                    );

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
