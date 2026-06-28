package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.dto.request.EvaluateQuestionRequest;
import com.p99softtraining.hiresense.dto.request.MarkKeyPointsRequest;
import com.p99softtraining.hiresense.dto.response.InterviewQuestionResponse;
import com.p99softtraining.hiresense.dto.response.InterviewSessionResponse;
import com.p99softtraining.hiresense.dto.response.KeyPointResponse;
import com.p99softtraining.hiresense.entity.*;
import com.p99softtraining.hiresense.enums.CandidateStatus;
import com.p99softtraining.hiresense.enums.SessionStatus;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.*;
import com.p99softtraining.hiresense.service.InterviewExecutionService;
import com.p99softtraining.hiresense.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewExecutionServiceImpl implements InterviewExecutionService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final SessionQuestionEvalRepository evalRepository;
    private final InterviewerAssignmentRepository assignmentRepository;
    private final CandidateRepository candidateRepository;
    private final InterviewRoundRepository roundRepository;
    private final KeyPointRepository keyPointRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final SecurityService securityService;

    @Override
    @Transactional
    public InterviewSessionResponse startSession(UUID candidateId, UUID roundId) {
        User interviewer = securityService.getCurrentUser();

        // Verify interviewer is assigned to this candidate
        boolean isAssigned = assignmentRepository
                .findByInterviewerIdAndHiringDrive_Company_IdOrderByCreatedAtDesc(
                        interviewer.getId(),
                        interviewer.getCompany().getId()
                )
                .stream()
                .anyMatch(a -> a.getCandidate().getId().equals(candidateId));

        if (!isAssigned) {
            throw new IllegalStateException("You are not assigned to this candidate.");
        }

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        InterviewRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found"));

        // Prevent duplicate session
        sessionRepository.findByInterviewerIdAndCandidateIdAndInterviewRoundId(
                interviewer.getId(), candidateId, roundId)
                .ifPresent(s -> {
                    throw new IllegalStateException("Session already exists for this interviewer/candidate/round.");
                });

        InterviewSession session = new InterviewSession();
        session.setInterviewer(interviewer);
        session.setCandidate(candidate);
        session.setInterviewRound(round);
        session.setStatus(SessionStatus.IN_PROGRESS);

        // Update candidate status
        if (candidate.getStatus() == CandidateStatus.ASSIGNED) {
            candidate.setStatus(CandidateStatus.INTERVIEW_IN_PROGRESS);
            candidateRepository.save(candidate);
        }

        return toResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSessionResponse getSession(UUID sessionId) {
        InterviewSession session = resolveSession(sessionId);
        return toResponse(session);
    }

    @Override
    @Transactional
    public InterviewSessionResponse markKeyPoints(UUID sessionId, UUID questionId, MarkKeyPointsRequest request) {
        InterviewSession session = resolveSession(sessionId);
        validateSessionOpen(session);

        // Validate question exists
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException("Question not found");
        }

        // Mark each requested key point as covered
        for (UUID kpId : request.getCoveredKeyPointIds()) {
            KeyPoint kp = keyPointRepository.findById(kpId)
                    .orElseThrow(() -> new ResourceNotFoundException("Key point not found: " + kpId));
            if (!kp.getInterviewQuestion().getId().equals(questionId)) {
                throw new IllegalArgumentException("Key point does not belong to the specified question.");
            }
            kp.setCovered(true);
            keyPointRepository.save(kp);
        }

        return toResponse(session);
    }

    @Override
    @Transactional
    public InterviewSessionResponse evaluateQuestion(UUID sessionId, UUID questionId, EvaluateQuestionRequest request) {
        InterviewSession session = resolveSession(sessionId);
        validateSessionOpen(session);

        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        SessionQuestionEval eval = evalRepository
                .findByInterviewSessionIdAndInterviewQuestionId(sessionId, questionId)
                .orElseGet(() -> {
                    SessionQuestionEval e = new SessionQuestionEval();
                    e.setInterviewSession(session);
                    e.setInterviewQuestion(question);
                    return e;
                });

        eval.setEvaluatorNotes(request.getEvaluatorNotes());
        eval.setAdditionalScore(request.getAdditionalScore());
        evalRepository.save(eval);

        return toResponse(session);
    }

    @Override
    @Transactional
    public InterviewSessionResponse submitSession(UUID sessionId) {
        InterviewSession session = resolveSession(sessionId);
        validateSessionOpen(session);

        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Compute and persist score for this session
        computeAndPersistScore(session);

        // Update candidate status if all sessions done
        Candidate candidate = session.getCandidate();
        candidate.setStatus(CandidateStatus.INTERVIEW_COMPLETED);
        candidateRepository.save(candidate);

        return toResponse(session);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private InterviewSession resolveSession(UUID sessionId) {
        User currentUser = securityService.getCurrentUser();
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Interviewers can only access their own sessions
        if (!session.getInterviewer().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Session not found");
        }
        return session;
    }

    private void validateSessionOpen(InterviewSession session) {
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Session is already completed and cannot be modified.");
        }
    }

    private void computeAndPersistScore(InterviewSession session) {
        UUID candidateId = session.getCandidate().getId();
        UUID hiringDriveId = session.getCandidate().getHiringDrive().getId();
        UUID roundId = session.getInterviewRound().getId();

        // Count covered key points for this round
        List<InterviewQuestion> questions = questionRepository
                .findByCandidateIdAndInterviewRoundIdOrderByDifficultyLevel(candidateId, roundId);

        int coveredCount = questions.stream()
                .flatMap(q -> q.getKeyPoints().stream())
                .mapToInt(kp -> kp.isCovered() ? 1 : 0)
                .sum();

        // Sum additional scores from session evals
        int additionalTotal = evalRepository.findByInterviewSessionId(session.getId())
                .stream()
                .mapToInt(SessionQuestionEval::getAdditionalScore)
                .sum();

        int sessionScore = coveredCount + additionalTotal;

        // Upsert EvaluationResult (add to existing total score across rounds)
        EvaluationResult result = evaluationResultRepository
                .findByCandidateIdAndHiringDriveId(candidateId, hiringDriveId)
                .orElseGet(() -> {
                    EvaluationResult r = new EvaluationResult();
                    r.setCandidate(session.getCandidate());
                    r.setHiringDrive(session.getCandidate().getHiringDrive());
                    r.setTotalScore(0);
                    return r;
                });

        result.setTotalScore(result.getTotalScore() + sessionScore);
        evaluationResultRepository.save(result);
    }

    private InterviewSessionResponse toResponse(InterviewSession session) {
        UUID candidateId = session.getCandidate().getId();
        UUID roundId = session.getInterviewRound().getId();

        List<InterviewQuestion> questions = questionRepository
                .findByCandidateIdAndInterviewRoundIdOrderByDifficultyLevel(candidateId, roundId);

        List<InterviewQuestionResponse> questionResponses = questions.stream()
                .map(q -> {
                    List<KeyPointResponse> kpResponses = q.getKeyPoints() == null
                            ? List.of()
                            : q.getKeyPoints().stream()
                                    .map(kp -> KeyPointResponse.builder()
                                            .id(kp.getId())
                                            .pointText(kp.getPointText())
                                            .covered(kp.isCovered())
                                            .build())
                                    .toList();

                    return InterviewQuestionResponse.builder()
                            .id(q.getId())
                            .questionText(q.getQuestionText())
                            .difficultyLevel(q.getDifficultyLevel())
                            .keyPoints(kpResponses)
                            .build();
                })
                .toList();

        return InterviewSessionResponse.builder()
                .id(session.getId())
                .candidateId(session.getCandidate().getId())
                .candidateFullName(session.getCandidate().getFullName())
                .interviewRoundId(session.getInterviewRound().getId())
                .interviewRoundName(session.getInterviewRound().getName())
                .status(session.getStatus())
                .completedAt(session.getCompletedAt())
                .questions(questionResponses)
                .build();
    }
}
