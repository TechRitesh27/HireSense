package com.p99softtraining.hiresense.dto.response;

import com.p99softtraining.hiresense.enums.ProfileStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CandidateProfileResponse {

    private UUID id;
    private UUID candidateId;
    private String candidateFullName;
    private List<String> skills;
    private List<CandidateProjectResponse> projects;
    private ProfileStatus status;
    private LocalDateTime parsedAt;
}
