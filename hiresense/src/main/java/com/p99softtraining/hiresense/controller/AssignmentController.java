package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.dto.request.AssignCandidateRequest;
import com.p99softtraining.hiresense.dto.response.AssignedCandidateResponse;
import com.p99softtraining.hiresense.dto.response.AssignmentResponse;
import com.p99softtraining.hiresense.service.AssignmentService;
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
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * Assign one or more interviewers to a specific candidate in a hiring drive.
     * Only Company Admins can perform this action.
     */
    @PostMapping("/api/v1/hiring-drives/{hiringDriveId}/candidates/{candidateId}/assignments")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<List<AssignmentResponse>> assignInterviewers(
            @PathVariable UUID hiringDriveId,
            @PathVariable UUID candidateId,
            @Valid @RequestBody AssignCandidateRequest request
    ) {
        return new ResponseEntity<>(
                assignmentService.assignInterviewers(hiringDriveId, candidateId, request),
                HttpStatus.CREATED
        );
    }

    /**
     * Get all interviewer-candidate assignments for a hiring drive.
     * Only Company Admins can view this.
     */
    @GetMapping("/api/v1/hiring-drives/{hiringDriveId}/assignments")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsForHiringDrive(
            @PathVariable UUID hiringDriveId
    ) {
        return ResponseEntity.ok(
                assignmentService.getAssignmentsForHiringDrive(hiringDriveId)
        );
    }

    /**
     * Get all candidates assigned to the currently authenticated interviewer.
     * Only Interviewers can access this endpoint.
     */
    @GetMapping("/api/v1/interviewers/me/assignments")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public ResponseEntity<List<AssignedCandidateResponse>> getMyAssignedCandidates() {
        return ResponseEntity.ok(
                assignmentService.getMyAssignedCandidates()
        );
    }

    /**
     * Remove a specific assignment.
     * Only Company Admins can perform this action.
     */
    @DeleteMapping("/api/v1/hiring-drives/{hiringDriveId}/candidates/{candidateId}/assignments/{assignmentId}")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable UUID hiringDriveId,
            @PathVariable UUID candidateId,
            @PathVariable UUID assignmentId
    ) {
        assignmentService.removeAssignment(hiringDriveId, candidateId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
