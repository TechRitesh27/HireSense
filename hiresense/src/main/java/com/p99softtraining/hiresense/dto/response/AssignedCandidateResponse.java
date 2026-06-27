package com.p99softtraining.hiresense.dto.response;

import com.p99softtraining.hiresense.enums.CandidateStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AssignedCandidateResponse {

    private UUID assignmentId;

    private UUID candidateId;

    private UUID hiringDriveId;
    private String hiringDriveTitle;

    private String fullName;
    private String email;
    private String phone;
    private String collegeName;
    private String degree;
    private String branch;
    private Integer graduationYear;
    private String resumeUrl;
    private CandidateStatus status;
}
