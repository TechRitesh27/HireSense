package com.p99softtraining.hiresense.dto.response;

import com.p99softtraining.hiresense.enums.DifficultyLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InterviewQuestionResponse {

    private UUID id;
    private String questionText;
    private DifficultyLevel difficultyLevel;
    private List<KeyPointResponse> keyPoints;
}
