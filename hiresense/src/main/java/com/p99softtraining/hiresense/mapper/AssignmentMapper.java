package com.p99softtraining.hiresense.mapper;

import com.p99softtraining.hiresense.dto.response.AssignedCandidateResponse;
import com.p99softtraining.hiresense.dto.response.AssignmentResponse;
import com.p99softtraining.hiresense.entity.Candidate;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.entity.InterviewerAssignment;
import com.p99softtraining.hiresense.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMapper {

    public AssignmentResponse toAssignmentResponse(InterviewerAssignment assignment){
        HiringDrive hiringDrive = assignment.getHiringDrive();
        Candidate candidate = assignment.getCandidate();
        User interviewer = assignment.getInterviewer();

        return AssignmentResponse.builder()
                .id(assignment.getId())
                .hiringDriveId(hiringDrive.getId())
                .hiringDriveTitle(hiringDrive.getTitle())
                .candidateId(candidate.getId())
                .candidateFullName(candidate.getFullName())
                .candidateEmail(candidate.getEmail())
                .interviewerId(interviewer.getId())
                .interviewerFullName(interviewer.getFullName())
                .interviewerEmail(interviewer.getEmail())
                .createdAt(assignment.getCreatedAt())
                .build();
    }

    public AssignedCandidateResponse toAssignedCandidateResponse(InterviewerAssignment assignment){
        Candidate candidate = assignment.getCandidate();
        HiringDrive hiringDrive = assignment.getHiringDrive();

        return AssignedCandidateResponse.builder()
                .candidateId(candidate.getId())
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .hiringDriveId(hiringDrive.getId())
                .hiringDriveName(hiringDrive.getTitle())
                .sessionId(null)
                .sessionStatus(null)
                .build();
    }
}
