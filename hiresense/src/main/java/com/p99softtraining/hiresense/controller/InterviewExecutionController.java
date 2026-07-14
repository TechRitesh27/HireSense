package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.dto.request.AddCustomQuestionRequest;
import com.p99softtraining.hiresense.dto.request.EvaluateQuestionRequest;
import com.p99softtraining.hiresense.dto.request.StartSessionRequest;
import com.p99softtraining.hiresense.dto.response.AssignedCandidateResponse;
import com.p99softtraining.hiresense.dto.response.InterviewQuestionResponse;
import com.p99softtraining.hiresense.dto.response.InterviewSessionResponse;
import com.p99softtraining.hiresense.service.InterviewExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InterviewExecutionController {

    private final InterviewExecutionService interviewExecutionService;

    /** Start a pre-created PENDING session: generate AI questions, transition to IN_PROGRESS */
    @PostMapping("/api/v1/sessions/{sessionId}/start")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<InterviewSessionResponse> startSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody StartSessionRequest request
    ) {
        return new ResponseEntity<>(
                interviewExecutionService.startSession(sessionId, request),
                HttpStatus.OK
        );
    }

    /** Get the session with all questions and their current evaluation state */
    @GetMapping("/api/v1/sessions/{sessionId}")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<InterviewSessionResponse> getSession(
            @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(
                interviewExecutionService.getSession(sessionId)
        );
    }

    /** Add a custom (non-AI) question to an IN_PROGRESS session */
    @PostMapping("/api/v1/sessions/{sessionId}/questions")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<InterviewQuestionResponse> addCustomQuestion(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AddCustomQuestionRequest request
    ) {
        return new ResponseEntity<>(
                interviewExecutionService.addCustomQuestion(sessionId, request),
                HttpStatus.CREATED
        );
    }

    /** Record or overwrite the notes and verdict for a question within a session */
    @PutMapping("/api/v1/sessions/{sessionId}/questions/{questionId}/evaluate")
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

    /** Submit session as complete — triggers scoring and round score recalculation */
    @PostMapping("/api/v1/sessions/{sessionId}/submit")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<InterviewSessionResponse> submitSession(
            @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(
                interviewExecutionService.submitSession(sessionId)
        );
    }

    /** Get all candidates assigned to the authenticated interviewer across active hiring drives */
    @GetMapping("/api/v1/interviewers/me/assigned-candidates")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<List<AssignedCandidateResponse>> getAssignedCandidates() {
        return ResponseEntity.ok(
                interviewExecutionService.getAssignedCandidates()
        );
    }
}
