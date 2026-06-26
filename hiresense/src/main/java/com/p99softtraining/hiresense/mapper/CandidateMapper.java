package com.p99softtraining.hiresense.mapper;

import com.p99softtraining.hiresense.dto.request.CreateCandidateRequest;
import com.p99softtraining.hiresense.dto.response.CandidateResponse;
import com.p99softtraining.hiresense.entity.Candidate;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.enums.CandidateStatus;
import org.springframework.stereotype.Component;

@Component
public class CandidateMapper {

    public CandidateResponse toResponse(Candidate candidate) {
        if (candidate == null) {
            return null;
        }

        HiringDrive hiringDrive = candidate.getHiringDrive();

        return CandidateResponse.builder()
                .id(candidate.getId())
                .hiringDriveId(hiringDrive != null ? hiringDrive.getId() : null)
                .hiringDriveTitle(hiringDrive != null ? hiringDrive.getTitle() : null)
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .collegeName(candidate.getCollegeName())
                .degree(candidate.getDegree())
                .branch(candidate.getBranch())
                .graduationYear(candidate.getGraduationYear())
                .resumeUrl(candidate.getResumeUrl())
                .status(candidate.getStatus())
                .build();
    }

    public Candidate toEntity(CreateCandidateRequest request, HiringDrive hiringDrive) {
        if (request == null) {
            return null;
        }

        Candidate candidate = new Candidate();
        candidate.setHiringDrive(hiringDrive);
        candidate.setFullName(request.getFullName());
        candidate.setEmail(request.getEmail());
        candidate.setPhone(request.getPhone());
        candidate.setCollegeName(request.getCollegeName());
        candidate.setDegree(request.getDegree());
        candidate.setBranch(request.getBranch());
        candidate.setGraduationYear(request.getGraduationYear());
        candidate.setResumeUrl(request.getResumeUrl());
        candidate.setStatus(CandidateStatus.IMPORTED);

        return candidate;
    }
}
