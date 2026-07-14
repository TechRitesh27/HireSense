package com.p99softtraining.hiresense.dto.response;

import com.p99softtraining.hiresense.enums.DifficultyLevel;
import com.p99softtraining.hiresense.enums.QuestionSource;
import com.p99softtraining.hiresense.enums.Verdict;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InterviewQuestionResponse {

    private UUID id;
    private String questionText;
    private DifficultyLevel difficultyLevel;
    private QuestionSource source;
    private String skill;
    private String notes;
    private Verdict verdict;
}
