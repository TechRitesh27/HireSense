package com.p99softtraining.hiresense.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RankedCandidateResponse {

    private int rank;
    private UUID candidateId;
    private String fullName;
    private String email;
    private int totalScore;
}
