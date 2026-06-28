package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.response.InterviewQuestionResponse;

import java.util.List;
import java.util.UUID;

public interface QuestionGenerationService {

    /** Generates (hardcoded demo) questions with key points for a candidate + round */
    List<InterviewQuestionResponse> generateQuestions(UUID candidateId, UUID roundId);

    /** Returns previously generated questions grouped by difficulty */
    List<InterviewQuestionResponse> getQuestions(UUID candidateId, UUID roundId);
}
