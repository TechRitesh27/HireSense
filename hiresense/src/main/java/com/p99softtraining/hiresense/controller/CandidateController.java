package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.dto.request.CreateCandidateRequest;
import com.p99softtraining.hiresense.dto.response.CandidateResponse;
import com.p99softtraining.hiresense.dto.response.ExcelUploadResponse;
import com.p99softtraining.hiresense.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hiring-drives/{hiringDriveId}/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<CandidateResponse> createCandidate(
            @PathVariable UUID hiringDriveId,
            @Valid @RequestBody CreateCandidateRequest request
    ) {

        return new ResponseEntity<>(
                candidateService.createCandidate(hiringDriveId, request),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<List<CandidateResponse>> getCandidates(
            @PathVariable UUID hiringDriveId
    ) {

        return ResponseEntity.ok(
                candidateService.getCandidatesForHiringDrive(hiringDriveId)
        );
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<ExcelUploadResponse> uploadCandidates(
            @PathVariable UUID hiringDriveId,
            @RequestParam("file") MultipartFile file
    ) {

        return ResponseEntity.ok(
                candidateService.uploadCandidatesFromExcel(
                        hiringDriveId,
                        file
                )
        );
    }

    @PostMapping("/import-url")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<ExcelUploadResponse> importCandidatesFromUrl(
            @PathVariable UUID hiringDriveId,
            @RequestParam("url") String url
    ) {

        return ResponseEntity.ok(
                candidateService.importCandidatesFromUrl(
                        hiringDriveId,
                        url
                )
        );
    }
}
