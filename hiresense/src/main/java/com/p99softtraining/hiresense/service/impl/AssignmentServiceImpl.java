package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.dto.request.AssignCandidateRequest;
import com.p99softtraining.hiresense.dto.response.AssignedCandidateResponse;
import com.p99softtraining.hiresense.dto.response.AssignmentResponse;
import com.p99softtraining.hiresense.entity.InterviewerAssignment;
import com.p99softtraining.hiresense.mapper.AssignmentMapper;
import com.p99softtraining.hiresense.repository.InterviewerAssignmentRepository;
import com.p99softtraining.hiresense.service.AssignmentService;
import com.p99softtraining.hiresense.entity.Candidate;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.entity.User;
import com.p99softtraining.hiresense.enums.CandidateStatus;
import com.p99softtraining.hiresense.enums.HiringDriveStatus;
import com.p99softtraining.hiresense.enums.Role;
import com.p99softtraining.hiresense.exception.ResourceAlreadyExistsException;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.CandidateRepository;
import com.p99softtraining.hiresense.repository.HiringDriveRepository;
import com.p99softtraining.hiresense.repository.UserRepository;
import com.p99softtraining.hiresense.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final InterviewerAssignmentRepository assignmentRepository;
    private final HiringDriveRepository hiringDriveRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final SecurityService securityService;
    private final AssignmentMapper assignmentMapper;

    @Override
    @Transactional
    public List<AssignmentResponse> assignInterviewers(
            UUID hiringDriveId,
            UUID candidateId,
            AssignCandidateRequest request
    ) {
        Company company = securityService.getCurrentUserCompany();
        HiringDrive hiringDrive = resolveCompanyHiringDrive(hiringDriveId, company.getId());
        validateHiringDriveIsActive(hiringDrive);

        Candidate candidate = resolveHiringDriveCandidate(candidateId, hiringDriveId);

        List<InterviewerAssignment> created = new ArrayList<>();

        for (UUID interviewerId : request.getInterviewerIds()) {
            User interviewer = userRepository.findById(interviewerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Interviewer not found with id: " + interviewerId));

            validateInterviewerRole(interviewer, interviewerId);
            validateInterviewerBelongsToCompany(interviewer, company.getId(), interviewerId);

            if (assignmentRepository.existsByHiringDriveIdAndInterviewerIdAndCandidateId(
                    hiringDriveId, interviewerId, candidateId)) {
                throw new ResourceAlreadyExistsException(
                        "Interviewer " + interviewerId + " is already assigned to this candidate");
            }

            InterviewerAssignment assignment = new InterviewerAssignment();
            assignment.setHiringDrive(hiringDrive);
            assignment.setInterviewer(interviewer);
            assignment.setCandidate(candidate);

            created.add(assignmentRepository.save(assignment));
        }

        // Promote candidate status only if still IMPORTED
        if (candidate.getStatus() == CandidateStatus.IMPORTED) {
            candidate.setStatus(CandidateStatus.ASSIGNED);
            candidateRepository.save(candidate);
        }

        return created.stream()
                .map(assignmentMapper::toAssignmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsForHiringDrive(UUID hiringDriveId) {
        Company company = securityService.getCurrentUserCompany();
        resolveCompanyHiringDrive(hiringDriveId, company.getId());

        return assignmentRepository.findByHiringDriveIdOrderByCreatedAtDesc(hiringDriveId)
                .stream()
                .map(assignmentMapper::toAssignmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedCandidateResponse> getMyAssignedCandidates() {
        User currentUser = securityService.getCurrentUser();
        Company company = securityService.getCurrentUserCompany();

        return assignmentRepository
                .findByInterviewerIdAndHiringDrive_Company_IdOrderByCreatedAtDesc(
                        currentUser.getId(),
                        company.getId()
                )
                .stream()
                .map(assignmentMapper::toAssignedCandidateResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeAssignment(UUID hiringDriveId, UUID candidateId, UUID assignmentId) {
        Company company = securityService.getCurrentUserCompany();
        resolveCompanyHiringDrive(hiringDriveId, company.getId());

        InterviewerAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        // Verify this assignment belongs to the given hiring drive and candidate
        if (!assignment.getHiringDrive().getId().equals(hiringDriveId) ||
                !assignment.getCandidate().getId().equals(candidateId)) {
            throw new ResourceNotFoundException("Assignment not found");
        }

        assignmentRepository.delete(assignment);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private HiringDrive resolveCompanyHiringDrive(UUID hiringDriveId, UUID companyId) {
        HiringDrive hiringDrive = hiringDriveRepository.findById(hiringDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hiring drive not found"));

        if (!hiringDrive.getCompany().getId().equals(companyId)) {
            // Intentionally return 404 to avoid information leakage
            throw new ResourceNotFoundException("Hiring drive not found");
        }

        return hiringDrive;
    }

    private void validateHiringDriveIsActive(HiringDrive hiringDrive) {
        if (hiringDrive.getStatus() != HiringDriveStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Assignments can only be created for ACTIVE hiring drives. " +
                            "Current status: " + hiringDrive.getStatus());
        }
    }

    private Candidate resolveHiringDriveCandidate(UUID candidateId, UUID hiringDriveId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (!candidate.getHiringDrive().getId().equals(hiringDriveId)) {
            throw new ResourceNotFoundException("Candidate does not belong to the specified hiring drive");
        }

        return candidate;
    }

    private void validateInterviewerRole(User user, UUID interviewerId) {
        if (user.getRole() != Role.INTERVIEWER) {
            throw new IllegalArgumentException(
                    "User " + interviewerId + " does not have the INTERVIEWER role");
        }
    }

    private void validateInterviewerBelongsToCompany(User interviewer, UUID companyId, UUID interviewerId) {
        if (interviewer.getCompany() == null ||
                !interviewer.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException(
                    "Interviewer " + interviewerId + " does not belong to the same company as the hiring drive");
        }
    }
}
