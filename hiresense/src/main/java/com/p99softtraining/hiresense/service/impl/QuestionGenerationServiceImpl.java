package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.config.CacheConfig;
import com.p99softtraining.hiresense.dto.response.InterviewQuestionResponse;
import com.p99softtraining.hiresense.dto.response.KeyPointResponse;
import com.p99softtraining.hiresense.entity.*;
import com.p99softtraining.hiresense.enums.DifficultyLevel;
import com.p99softtraining.hiresense.enums.ProfileStatus;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.*;
import com.p99softtraining.hiresense.service.QuestionGenerationService;
import com.p99softtraining.hiresense.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionGenerationServiceImpl implements QuestionGenerationService {

    private final CandidateRepository candidateRepository;
    private final CandidateProfileRepository profileRepository;
    private final InterviewRoundRepository roundRepository;
    private final InterviewQuestionRepository questionRepository;
    private final SecurityService securityService;

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.QUESTIONS, key = "#candidateId + '_' + #roundId")
    public List<InterviewQuestionResponse> generateQuestions(UUID candidateId, UUID roundId) {
        securityService.getCurrentUserCompany();

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        CandidateProfile profile = profileRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume has not been parsed. Trigger parse-resume first."));

        if (profile.getStatus() != ProfileStatus.PARSED) {
            throw new IllegalStateException("Resume profile is not in PARSED state.");
        }

        InterviewRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found"));

        // Delete existing questions for this candidate-round pair (regenerate)
        if (questionRepository.existsByCandidateIdAndInterviewRoundId(candidateId, roundId)) {
            questionRepository.deleteByCandidateIdAndInterviewRoundId(candidateId, roundId);
        }

        // ── HARDCODED DEMO QUESTIONS ─────────────────────────────────────────
        List<InterviewQuestion> questions = buildDemoQuestions(candidate, round);
        // ─────────────────────────────────────────────────────────────────────

        List<InterviewQuestion> saved = questionRepository.saveAll(questions);
        return saved.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.QUESTIONS, key = "#candidateId + '_' + #roundId")
    public List<InterviewQuestionResponse> getQuestions(UUID candidateId, UUID roundId) {
        securityService.getCurrentUserCompany();

        return questionRepository
                .findByCandidateIdAndInterviewRoundIdOrderByDifficultyLevel(candidateId, roundId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<InterviewQuestion> buildDemoQuestions(Candidate candidate, InterviewRound round) {
        List<InterviewQuestion> questions = new ArrayList<>();

        questions.add(buildQuestion(candidate, round, DifficultyLevel.EASY,
                "What is the difference between JDK, JRE, and JVM?",
                List.of(
                        "JVM executes bytecode",
                        "JRE includes JVM + libraries",
                        "JDK includes JRE + development tools"
                )));

        questions.add(buildQuestion(candidate, round, DifficultyLevel.EASY,
                "What are the main principles of OOP?",
                List.of(
                        "Encapsulation",
                        "Inheritance",
                        "Polymorphism",
                        "Abstraction"
                )));

        questions.add(buildQuestion(candidate, round, DifficultyLevel.MEDIUM,
                "Explain the Spring Boot auto-configuration mechanism.",
                List.of(
                        "Uses @EnableAutoConfiguration",
                        "Reads META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
                        "Conditionally applies configuration using @ConditionalOn* annotations",
                        "Can be overridden with custom beans"
                )));

        questions.add(buildQuestion(candidate, round, DifficultyLevel.MEDIUM,
                "How does Spring Security handle JWT authentication?",
                List.of(
                        "Filter chain intercepts incoming requests",
                        "JWT is extracted from Authorization header",
                        "Token is validated and UsernamePasswordAuthenticationToken is set in SecurityContext",
                        "Stateless session management"
                )));

        questions.add(buildQuestion(candidate, round, DifficultyLevel.HARD,
                "Explain database transaction isolation levels and when you would use each.",
                List.of(
                        "READ_UNCOMMITTED — dirty reads possible",
                        "READ_COMMITTED — prevents dirty reads",
                        "REPEATABLE_READ — prevents non-repeatable reads",
                        "SERIALIZABLE — fully isolated, highest overhead",
                        "Practical trade-off between consistency and performance"
                )));

        questions.add(buildQuestion(candidate, round, DifficultyLevel.HARD,
                "How would you design a scalable REST API for the e-commerce platform you built?",
                List.of(
                        "Stateless design with JWT",
                        "Pagination for list endpoints",
                        "Caching strategy (Redis/ETag)",
                        "Rate limiting",
                        "API versioning"
                )));

        return questions;
    }

    private InterviewQuestion buildQuestion(
            Candidate candidate,
            InterviewRound round,
            DifficultyLevel level,
            String text,
            List<String> keyPointTexts
    ) {
        InterviewQuestion q = new InterviewQuestion();
        q.setCandidate(candidate);
        q.setInterviewRound(round);
        q.setQuestionText(text);
        q.setDifficultyLevel(level);

        List<KeyPoint> keyPoints = keyPointTexts.stream().map(kpText -> {
            KeyPoint kp = new KeyPoint();
            kp.setInterviewQuestion(q);
            kp.setPointText(kpText);
            kp.setCovered(false);
            return kp;
        }).toList();

        q.setKeyPoints(keyPoints);
        return q;
    }

    private InterviewQuestionResponse toResponse(InterviewQuestion q) {
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
    }
}
