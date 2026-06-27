package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.EvaluateQuestionRequest;
import com.p99softtraining.hiresense.dto.request.MarkKeyPointsRequest;
import com.p99softtraining.hiresense.dto.response.InterviewSessionResponse;

import java.util.UUID;

public interface InterviewExecutionService {

    /** Starts a new interview session for an assigned candidate in a given round */
    InterviewSessionResponse startSession(UUID candidateId, UUID roundId);

    /** Returns the active or completed session with questions and key point state */
    InterviewSessionResponse getSession(UUID sessionId);

    /** Marks specific key points as covered within a session */
    InterviewSessionResponse markKeyPoints(UUID sessionId, UUID questionId, MarkKeyPointsRequest request);

    /** Saves evaluator notes and additional score for a question */
    InterviewSessionResponse evaluateQuestion(UUID sessionId, UUID questionId, EvaluateQuestionRequest request);

    /** Submits the session as complete and triggers scoring */
    InterviewSessionResponse submitSession(UUID sessionId);
}
