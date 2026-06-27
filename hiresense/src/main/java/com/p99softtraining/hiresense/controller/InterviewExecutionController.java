package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.dto.request.EvaluateQuestionRequest;
import com.p99softtraining.hiresense.dto.request.MarkKeyPointsRequest;
import com.p99softtraining.hiresense.dto.response.InterviewSessionResponse;
import com.p99softtraining.hiresense.service.InterviewExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InterviewExecutionController {

    private final InterviewExecutionService interviewExecutionService;

    /** Start a new session for an assigned candidate in a round */
    @PostMapping("/api/v1/candidates/{candidateId}/rounds/{roundId}/sessions")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<InterviewSessionResponse> startSession(
            @PathVariable UUID candidateId,
            @PathVariable UUID roundId
    ) {
        return new ResponseEntity<>(
                interviewExecutionService.startSession(candidateId, roundId),
                HttpStatus.CREATED
        );
    }

    /** Get the session with all questions and current key point state */
    @GetMapping("/api/v1/sessions/{sessionId}")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<InterviewSessionResponse> getSession(
            @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(
                interviewExecutionService.getSession(sessionId)
        );
    }

    /** Mark key points as covered for a specific question */
    @PatchMapping("/api/v1/sessions/{sessionId}/questions/{questionId}/key-points")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<InterviewSessionResponse> markKeyPoints(
            @PathVariable UUID sessionId,
            @PathVariable UUID questionId,
            @Valid @RequestBody MarkKeyPointsRequest request
    ) {
        return ResponseEntity.ok(
                interviewExecutionService.markKeyPoints(sessionId, questionId, request)
        );
    }

    /** Save evaluator notes and additional score for a question */
    @PatchMapping("/api/v1/sessions/{sessionId}/questions/{questionId}/evaluate")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<InterviewSessionResponse> evaluateQuestion(
            @PathVariable UUID sessionId,
            @PathVariable UUID questionId,
            @Valid @RequestBody EvaluateQuestionRequest request
    ) {
        return ResponseEntity.ok(
                interviewExecutionService.evaluateQuestion(sessionId, questionId, request)
        );
    }

    /** Submit session as complete — triggers scoring */
    @PostMapping("/api/v1/sessions/{sessionId}/submit")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<InterviewSessionResponse> submitSession(
            @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(
                interviewExecutionService.submitSession(sessionId)
        );
    }
}
