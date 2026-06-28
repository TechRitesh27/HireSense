package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.dto.response.RankedCandidateResponse;
import com.p99softtraining.hiresense.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hiring-drives/{hiringDriveId}/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<List<RankedCandidateResponse>> getRankedResults(
            @PathVariable UUID hiringDriveId
    ) {
        return ResponseEntity.ok(
                rankingService.getRankedResults(hiringDriveId)
        );
    }
}
