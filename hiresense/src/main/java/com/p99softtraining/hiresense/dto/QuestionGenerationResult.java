package com.p99softtraining.hiresense.dto;

import java.util.List;

public record QuestionGenerationResult(
        List<AiQuestion> questions
) {
    public record AiQuestion(
            String questionText,
            String difficultyLevel,
            List<String> keyPoints
    ) {}
}
