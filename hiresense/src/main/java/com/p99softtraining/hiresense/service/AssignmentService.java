package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.AssignCandidateRequest;
import com.p99softtraining.hiresense.dto.response.AssignedCandidateResponse;
import com.p99softtraining.hiresense.dto.response.AssignmentResponse;

import java.util.List;
import java.util.UUID;

public interface AssignmentService {

    /**
     * Assigns one or more interviewers to a specific candidate in a hiring drive.
     * Only allowed when the hiring drive is ACTIVE and the candidate belongs to it.
     * Each interviewer must belong to the same company as the hiring drive.
     */
    List<AssignmentResponse> assignInterviewers(
            UUID hiringDriveId,
            UUID candidateId,
            AssignCandidateRequest request
    );

    /**
     * Returns all assignments for a hiring drive, visible to the company admin.
     */
    List<AssignmentResponse> getAssignmentsForHiringDrive(UUID hiringDriveId);

    /**
     * Returns all candidates assigned to the currently authenticated interviewer,
     * scoped to active hiring drives of their company.
     */
    List<AssignedCandidateResponse> getMyAssignedCandidates();

    /**
     * Removes a specific assignment. Only the company admin can perform this.
     */
    void removeAssignment(UUID hiringDriveId, UUID candidateId, UUID assignmentId);
}
