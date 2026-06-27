package com.p99softtraining.hiresense.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AssignmentResponse {

    private UUID id;

    private UUID hiringDriveId;
    private String hiringDriveTitle;

    private UUID candidateId;
    private String candidateFullName;
    private String candidateEmail;

    private UUID interviewerId;
    private String interviewerFullName;
    private String interviewerEmail;

    private LocalDateTime createdAt;

}
