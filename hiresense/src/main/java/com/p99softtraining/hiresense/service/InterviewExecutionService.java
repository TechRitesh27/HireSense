package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.AddCustomQuestionRequest;
import com.p99softtraining.hiresense.dto.request.EvaluateQuestionRequest;
import com.p99softtraining.hiresense.dto.request.StartSessionRequest;
import com.p99softtraining.hiresense.dto.response.AssignedCandidateResponse;
import com.p99softtraining.hiresense.dto.response.InterviewQuestionResponse;
import com.p99softtraining.hiresense.dto.response.InterviewSessionResponse;

import java.util.List;
import java.util.UUID;

public interface InterviewExecutionService {

    /**
     * Starts a pre-created PENDING session: generates AI questions, transitions to IN_PROGRESS.
     * Requires the calling INTERVIEWER to be assigned to the session's candidate.
     */
    InterviewSessionResponse startSession(UUID sessionId, StartSessionRequest request);

    /**
     * Returns the full session with all questions and their current evaluation state.
     * Only accessible by the session's own interviewer.
     */
    InterviewSessionResponse getSession(UUID sessionId);

    /**
     * Adds a custom (non-AI) question to an IN_PROGRESS session.
     * Only accessible by the session's own interviewer.
     */
    InterviewQuestionResponse addCustomQuestion(UUID sessionId, AddCustomQuestionRequest request);

    /**
     * Records or overwrites the notes and verdict for a question within a session.
     * Session must be IN_PROGRESS and the question must belong to the session.
     */
    InterviewSessionResponse evaluateQuestion(UUID sessionId, UUID questionId, EvaluateQuestionRequest request);

    /**
     * Submits the session: computes sessionScore, persists completedAt, transitions to COMPLETED,
     * and triggers round score recalculation.
     * Only accessible by the session's own interviewer.
     */
    InterviewSessionResponse submitSession(UUID sessionId);

    /**
     * Returns all candidates assigned to the authenticated INTERVIEWER across active hiring drives,
     * including each candidate's current session ID and status (if a session exists).
     */
    List<AssignedCandidateResponse> getAssignedCandidates();
}
