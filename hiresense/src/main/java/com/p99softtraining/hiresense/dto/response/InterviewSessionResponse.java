package com.p99softtraining.hiresense.dto.response;

import com.p99softtraining.hiresense.enums.DifficultyLevel;
import com.p99softtraining.hiresense.enums.SessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InterviewSessionResponse {

    private UUID id;
    private UUID candidateId;
    private String candidateFullName;
    private UUID interviewRoundId;
    private String interviewRoundName;
    private SessionStatus status;
    private LocalDateTime completedAt;
    private DifficultyLevel difficultyLevel;
    private Integer questionCount;
    private Double sessionScore;
    private List<InterviewQuestionResponse> questions;
}
