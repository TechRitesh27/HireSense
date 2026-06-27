package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.dto.response.CandidateProfileResponse;
import com.p99softtraining.hiresense.service.ResumeIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates/{candidateId}")
@RequiredArgsConstructor
public class ResumeIntelligenceController {

    private final ResumeIntelligenceService resumeIntelligenceService;

    @PostMapping("/parse-resume")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<CandidateProfileResponse> parseResume(
            @PathVariable UUID candidateId
    ) {
        return ResponseEntity.ok(
                resumeIntelligenceService.parseResume(candidateId)
        );
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'INTERVIEWER')")
    public ResponseEntity<CandidateProfileResponse> getProfile(
            @PathVariable UUID candidateId
    ) {
        return ResponseEntity.ok(
                resumeIntelligenceService.getProfile(candidateId)
        );
    }
}
