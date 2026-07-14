package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.dto.GeneratedQuestion;
import com.p99softtraining.hiresense.dto.request.AddCustomQuestionRequest;
import com.p99softtraining.hiresense.dto.request.EvaluateQuestionRequest;
import com.p99softtraining.hiresense.dto.request.StartSessionRequest;
import com.p99softtraining.hiresense.dto.response.AssignedCandidateResponse;
import com.p99softtraining.hiresense.dto.response.InterviewQuestionResponse;
import com.p99softtraining.hiresense.dto.response.InterviewSessionResponse;
import com.p99softtraining.hiresense.entity.*;
import com.p99softtraining.hiresense.enums.HiringDriveStatus;
import com.p99softtraining.hiresense.enums.ProfileStatus;
import com.p99softtraining.hiresense.enums.QuestionSource;
import com.p99softtraining.hiresense.enums.SessionStatus;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.*;
import com.p99softtraining.hiresense.service.AiQuestionGenerator;
import com.p99softtraining.hiresense.service.InterviewExecutionService;
import com.p99softtraining.hiresense.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewExecutionServiceImpl implements InterviewExecutionService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final SessionQuestionEvalRepository evalRepository;
    private final InterviewerAssignmentRepository assignmentRepository;
    private final CandidateRepository candidateRepository;
    private final InterviewRoundRepository roundRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final CandidateProfileRepository profileRepository;
    private final AiQuestionGenerator aiQuestionGenerator;
    private final SecurityService securityService;

    @Override
    @Transactional
    public InterviewSessionResponse startSession(UUID sessionId, StartSessionRequest request) {
        // 1. Load session (404 if not found)
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // 2. Get the authenticated interviewer
        User interviewer = securityService.getCurrentUser();

        // 3. Verify interviewer is assigned to this session's candidate in the hiring drive (403 if not)
        UUID hiringDriveId = session.getInterviewRound().getHiringDrive().getId();
        UUID candidateId = session.getCandidate().getId();
        boolean isAssigned = assignmentRepository.existsByHiringDriveIdAndInterviewerIdAndCandidateId(
                hiringDriveId, interviewer.getId(), candidateId);

        if (!isAssigned) {
            throw new AccessDeniedException("You are not assigned to this candidate in the hiring drive.");
        }

        // 4. Validate session state is PENDING (409 if not)
        if (session.getStatus() != SessionStatus.PENDING) {
            throw new IllegalStateException("Session cannot be started: current status is " + session.getStatus());
        }

        // 5. Validate CandidateProfile exists and is PARSED (422 if not)
        CandidateProfile profile = profileRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatusCode.valueOf(422),
                        "Candidate profile not found. The resume must be parsed before starting the session."));

        if (profile.getStatus() != ProfileStatus.PARSED) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(422),
                    "Candidate profile is not yet parsed (current status: " + profile.getStatus() + "). " +
                    "The resume must be parsed before starting the session.");
        }

        // 6. Persist difficultyLevel and questionCount on the session
        session.setDifficultyLevel(request.getDifficultyLevel());
        session.setQuestionCount(request.getQuestionCount());

        // 7. Call AiQuestionGenerator.generate(primarySkills, secondarySkills, difficulty, count)
        List<String> primarySkills = parseSkills(profile.getPrimarySkills());
        List<String> secondarySkills = parseSkills(profile.getSecondarySkills());

        List<GeneratedQuestion> generatedQuestions = aiQuestionGenerator.generate(
                primarySkills, secondarySkills, request.getDifficultyLevel(), request.getQuestionCount());

        // 8. Persist each GeneratedQuestion as an InterviewQuestion with source=AI_GENERATED
        for (GeneratedQuestion gq : generatedQuestions) {
            InterviewQuestion question = new InterviewQuestion();
            question.setInterviewSession(session);
            question.setQuestionText(gq.questionText());
            question.setDifficultyLevel(gq.difficultyLevel());
            question.setSource(QuestionSource.AI_GENERATED);
            question.setSkill(gq.skill());
            questionRepository.save(question);
        }

        // 9. Transition session to IN_PROGRESS and save
        session.setStatus(SessionStatus.IN_PROGRESS);
        sessionRepository.save(session);

        // 10. Return InterviewSessionResponse with all questions
        return toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSessionResponse getSession(UUID sessionId) {
        InterviewSession session = resolveSession(sessionId);
        return toResponse(session);
    }

    @Override
    @Transactional
    public InterviewSessionResponse evaluateQuestion(UUID sessionId, UUID questionId, EvaluateQuestionRequest request) {
        InterviewSession session = resolveSession(sessionId);
        validateSessionOpen(session);

        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        // Req 5.5/5.6: verify the question belongs to this session
        if (question.getInterviewSession() == null ||
            !question.getInterviewSession().getId().equals(sessionId)) {
            throw new ResourceNotFoundException("Question not found in this session");
        }

        SessionQuestionEval eval = evalRepository
                .findByInterviewSessionIdAndInterviewQuestionId(sessionId, questionId)
                .orElseGet(() -> {
                    SessionQuestionEval e = new SessionQuestionEval();
                    e.setInterviewSession(session);
                    e.setInterviewQuestion(question);
                    return e;
                });

        eval.setNotes(request.getNotes());
        eval.setVerdict(request.getVerdict());
        evalRepository.save(eval);

        return toResponse(session);
    }

    @Override
    @Transactional
    public InterviewSessionResponse submitSession(UUID sessionId) {
        // 1. Load session — 404 if not found, 403 if not the session's interviewer
        InterviewSession session = resolveSession(sessionId);

        // 2. Verify session is IN_PROGRESS — 409 if not
        validateSessionOpen(session);

        // 3. Compute sessionScore from evaluated questions
        List<SessionQuestionEval> evals = evalRepository.findByInterviewSessionId(session.getId());
        List<SessionQuestionEval> evaluated = evals.stream()
                .filter(e -> e.getVerdict() != null)
                .toList();

        double sessionScore;
        if (evaluated.isEmpty()) {
            sessionScore = 0.0;
        } else {
            int earnedPoints = evaluated.stream().mapToInt(e -> e.getVerdict().points()).sum();
            int maxPoints = evaluated.size() * 3;
            sessionScore = Math.round((earnedPoints / (double) maxPoints) * 100.0 * 100.0) / 100.0;
        }

        // 4. Persist sessionScore, completedAt, and COMPLETED status
        session.setSessionScore(sessionScore);
        session.setCompletedAt(LocalDateTime.now());
        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);

        // 5. Trigger round score recalculation
        recalculateRoundScore(session);

        // 6. Return InterviewSessionResponse
        return toResponse(session);
    }

    @Override
    @Transactional
    public InterviewQuestionResponse addCustomQuestion(UUID sessionId, AddCustomQuestionRequest request) {
        // 1. Resolve session: verifies INTERVIEWER owns it (403 if not), 404 if not found
        InterviewSession session = resolveSession(sessionId);

        // 2. Verify session is IN_PROGRESS (409 if not)
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Cannot add question: session is not IN_PROGRESS (current status: " + session.getStatus() + ")");
        }

        // 3. Persist InterviewQuestion with source=CUSTOM
        InterviewQuestion question = new InterviewQuestion();
        question.setInterviewSession(session);
        question.setQuestionText(request.getQuestionText());
        question.setDifficultyLevel(request.getDifficultyLevel());
        question.setSource(QuestionSource.CUSTOM);
        questionRepository.save(question);

        // 4. Return InterviewQuestionResponse
        return InterviewQuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .difficultyLevel(question.getDifficultyLevel())
                .source(question.getSource())
                .skill(null)
                .notes(null)
                .verdict(null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedCandidateResponse> getAssignedCandidates() {
        // 1. Get the currently authenticated INTERVIEWER
        User interviewer = securityService.getCurrentUser();

        // 2. Fetch all InterviewerAssignment records for this interviewer
        List<InterviewerAssignment> assignments = assignmentRepository.findByInterviewerId(interviewer.getId());

        // 3. Filter to only active hiring drives, then map each to AssignedCandidateResponse
        return assignments.stream()
                .filter(a -> a.getHiringDrive().getStatus() == HiringDriveStatus.ACTIVE)
                .map(assignment -> {
                    HiringDrive drive = assignment.getHiringDrive();
                    Candidate candidate = assignment.getCandidate();

                    // Find the interviewer's session for this candidate in this hiring drive (if any)
                    InterviewSession session = sessionRepository
                            .findByCandidateIdAndInterviewerIdAndInterviewRound_HiringDriveId(
                                    candidate.getId(), interviewer.getId(), drive.getId())
                            .orElse(null);

                    return AssignedCandidateResponse.builder()
                            .candidateId(candidate.getId())
                            .fullName(candidate.getFullName())
                            .email(candidate.getEmail())
                            .hiringDriveId(drive.getId())
                            .hiringDriveName(drive.getTitle())
                            .sessionId(session != null ? session.getId() : null)
                            .sessionStatus(session != null ? session.getStatus() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Splits a comma-separated skills string into a trimmed list.
     * Returns an empty list if the input is null or blank.
     */
    private List<String> parseSkills(String skillsCsv) {
        if (skillsCsv == null || skillsCsv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(skillsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private InterviewSession resolveSession(UUID sessionId) {
        User currentUser = securityService.getCurrentUser();
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Interviewers can only access their own sessions (403 if not the session's designated interviewer)
        if (!session.getInterviewer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not the designated interviewer for this session.");
        }
        return session;
    }

    private void validateSessionOpen(InterviewSession session) {
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is not IN_PROGRESS (current status: " + session.getStatus() + ")");
        }
    }

    /**
     * Recalculates the round score for the candidate+round of the given session
     * by averaging all COMPLETED session scores, then upserts the EvaluationResult.
     */
    private void recalculateRoundScore(InterviewSession session) {
        UUID candidateId = session.getCandidate().getId();
        UUID interviewRoundId = session.getInterviewRound().getId();
        UUID hiringDriveId = session.getInterviewRound().getHiringDrive().getId();

        // Fetch all COMPLETED sessions for this candidate in this round
        List<InterviewSession> completedSessions = sessionRepository
                .findByCandidateIdAndInterviewRoundIdAndStatus(
                        candidateId, interviewRoundId, SessionStatus.COMPLETED);

        double roundScore = completedSessions.stream()
                .filter(s -> s.getSessionScore() != null)
                .mapToDouble(InterviewSession::getSessionScore)
                .average()
                .orElse(0.0);

        // Round to 2 decimal places
        roundScore = Math.round(roundScore * 100.0) / 100.0;

        // Upsert EvaluationResult keyed by (candidate, hiringDrive, interviewRound)
        EvaluationResult result = evaluationResultRepository
                .findByCandidateIdAndHiringDriveIdAndInterviewRoundId(
                        candidateId, hiringDriveId, interviewRoundId)
                .orElseGet(() -> {
                    EvaluationResult r = new EvaluationResult();
                    r.setCandidate(session.getCandidate());
                    r.setHiringDrive(session.getInterviewRound().getHiringDrive());
                    r.setInterviewRound(session.getInterviewRound());
                    return r;
                });

        result.setTotalScore(roundScore);
        evaluationResultRepository.save(result);
    }

    private InterviewSessionResponse toResponse(InterviewSession session) {
        List<InterviewQuestion> questions = questionRepository
                .findByInterviewSessionIdOrderByDifficultyLevel(session.getId());

        // Load evals for this session to populate notes and verdict on each question
        List<SessionQuestionEval> evals = evalRepository.findByInterviewSessionId(session.getId());

        List<InterviewQuestionResponse> questionResponses = questions.stream()
                .map(q -> {
                    // Find the eval for this question, if any
                    SessionQuestionEval eval = evals.stream()
                            .filter(e -> e.getInterviewQuestion().getId().equals(q.getId()))
                            .findFirst()
                            .orElse(null);

                    return InterviewQuestionResponse.builder()
                            .id(q.getId())
                            .questionText(q.getQuestionText())
                            .difficultyLevel(q.getDifficultyLevel())
                            .source(q.getSource())
                            .skill(q.getSkill())
                            .notes(eval != null ? eval.getNotes() : null)
                            .verdict(eval != null ? eval.getVerdict() : null)
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
                .difficultyLevel(session.getDifficultyLevel())
                .questionCount(session.getQuestionCount())
                .sessionScore(session.getSessionScore())
                .completedAt(session.getCompletedAt())
                .questions(questionResponses)
                .build();
    }
}
