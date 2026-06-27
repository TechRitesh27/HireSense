package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.dto.request.CreateHiringDriveRequest;
import com.p99softtraining.hiresense.dto.response.HiringDriveResponse;
import com.p99softtraining.hiresense.service.HiringDriveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hiring-drives")
@RequiredArgsConstructor
public class HiringDriveController {

    private final HiringDriveService hiringDriveService;

    @PostMapping
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<HiringDriveResponse> createHiringDrive(
            @Valid @RequestBody CreateHiringDriveRequest request
    ) {

        return new ResponseEntity<>(
                hiringDriveService.createHiringDrive(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'INTERVIEWER')")
    public ResponseEntity<List<HiringDriveResponse>> getHiringDrives() {

        return ResponseEntity.ok(
                hiringDriveService.getHiringDrivesForCurrentCompany()
        );
    }
}
