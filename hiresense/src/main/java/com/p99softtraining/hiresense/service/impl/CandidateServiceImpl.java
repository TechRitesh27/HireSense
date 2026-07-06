package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.dto.request.CreateCandidateRequest;
import com.p99softtraining.hiresense.dto.response.CandidateResponse;
import com.p99softtraining.hiresense.dto.response.ExcelUploadResponse;
import com.p99softtraining.hiresense.dto.response.RowErrorResponse;
import com.p99softtraining.hiresense.entity.Candidate;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.exception.ResourceAlreadyExistsException;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.mapper.CandidateMapper;
import com.p99softtraining.hiresense.parser.CandidateFileParser;
import com.p99softtraining.hiresense.parser.ParsedCandidateData;
import com.p99softtraining.hiresense.parser.ParsedRow;
import com.p99softtraining.hiresense.repository.CandidateRepository;
import com.p99softtraining.hiresense.repository.HiringDriveRepository;
import com.p99softtraining.hiresense.service.CandidateService;
import com.p99softtraining.hiresense.service.SecurityService;
import com.p99softtraining.hiresense.service.SpreadsheetDownloader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final HiringDriveRepository hiringDriveRepository;
    private final SecurityService securityService;
    private final CandidateMapper candidateMapper;
    private final List<CandidateFileParser> fileParsers;
    private final List<SpreadsheetDownloader> spreadsheetDownloaders;

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

        Candidate candidate = candidateMapper.toEntity(request, hiringDrive);

        return candidateMapper.toResponse(candidateRepository.save(candidate));
    }

    @Override
    public List<CandidateResponse> getCandidatesForHiringDrive(UUID hiringDriveId) {

        HiringDrive hiringDrive = getCompanyHiringDrive(hiringDriveId);

        return candidateRepository.findByHiringDriveIdOrderByCreatedAtDesc(hiringDrive.getId())
                .stream()
                .map(candidateMapper::toResponse)
                .toList();
    }

    private HiringDrive getCompanyHiringDrive(UUID hiringDriveId) {

        Company company = securityService.getCurrentUserCompany();

        HiringDrive hiringDrive = hiringDriveRepository.findById(hiringDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hiring drive not found"));

        if (!hiringDrive.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("Hiring drive not found");
        }

        return hiringDrive;
    }

    @Override
    public ExcelUploadResponse uploadCandidatesFromExcel(
            UUID hiringDriveId,
            MultipartFile file
    ) {

        CandidateFileParser parser = fileParsers.stream()
                .filter(p -> p.supports(file.getContentType(), file.getOriginalFilename()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type. Please upload an Excel file."));

        ParsedCandidateData parsedData;
        try {
            parsedData = parser.parse(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process Excel file", e);
        }

        return processParsedData(hiringDriveId, parsedData);
    }

    @Override
    public ExcelUploadResponse importCandidatesFromUrl(
            UUID hiringDriveId,
            String url
    ) {

        SpreadsheetDownloader downloader = spreadsheetDownloaders.stream()
                .filter(d -> d.supports(url))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported URL format. Please provide a valid HTTP/HTTPS spreadsheet link."));

        CandidateFileParser parser = fileParsers.stream()
                .filter(p -> p.supports("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", url))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No suitable spreadsheet parser found for this import."));

        ParsedCandidateData parsedData;
        try (java.io.InputStream inputStream = downloader.download(url)) {
            parsedData = parser.parse(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download or parse spreadsheet from URL: " + e.getMessage(), e);
        }

        return processParsedData(hiringDriveId, parsedData);
    }

    private ExcelUploadResponse processParsedData(UUID hiringDriveId, ParsedCandidateData parsedData) {
        if (!parsedData.getMissingColumns().isEmpty()) {
            return ExcelUploadResponse.builder()
                    .totalRows(0)
                    .successCount(0)
                    .failedCount(0)
                    .missingColumns(parsedData.getMissingColumns())
                    .errors(List.of())
                    .build();
        }

        int successCount = 0;
        int failedCount = 0;
        List<RowErrorResponse> errors = new ArrayList<>();

        for (ParsedRow row : parsedData.getRows()) {
            if (row.getErrorMessage() != null) {
                failedCount++;
                errors.add(new RowErrorResponse(row.getRowNumber(), row.getErrorMessage()));
                continue;
            }

            try {
                createCandidate(hiringDriveId, row.getRequest());
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add(new RowErrorResponse(row.getRowNumber(), e.getMessage()));
            }
        }

        return ExcelUploadResponse.builder()
                .totalRows(parsedData.getTotalRows())
                .successCount(successCount)
                .failedCount(failedCount)
                .missingColumns(parsedData.getMissingColumns())
                .errors(errors)
                .build();
    }
}
