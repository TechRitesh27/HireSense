package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.dto.response.InterviewQuestionResponse;
import com.p99softtraining.hiresense.service.QuestionGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/rounds/{roundId}/questions")
@RequiredArgsConstructor
public class QuestionGenerationController {

    private final QuestionGenerationService questionGenerationService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<List<InterviewQuestionResponse>> generateQuestions(
            @PathVariable UUID candidateId,
            @PathVariable UUID roundId
    ) {
        return ResponseEntity.ok(
                questionGenerationService.generateQuestions(candidateId, roundId)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'INTERVIEWER')")
    public ResponseEntity<List<InterviewQuestionResponse>> getQuestions(
            @PathVariable UUID candidateId,
            @PathVariable UUID roundId
    ) {
        return ResponseEntity.ok(
                questionGenerationService.getQuestions(candidateId, roundId)
        );
    }
}
