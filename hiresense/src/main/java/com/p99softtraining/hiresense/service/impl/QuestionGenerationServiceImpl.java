package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.config.CacheConfig;
import com.p99softtraining.hiresense.dto.response.InterviewQuestionResponse;
import com.p99softtraining.hiresense.dto.response.KeyPointResponse;
import com.p99softtraining.hiresense.entity.*;
import com.p99softtraining.hiresense.enums.DifficultyLevel;
import com.p99softtraining.hiresense.enums.ProfileStatus;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.*;
import com.p99softtraining.hiresense.dto.QuestionGenerationResult;
import com.p99softtraining.hiresense.service.QuestionGenerationService;
import com.p99softtraining.hiresense.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
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
    private final ChatClient chatClient;

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

        // Generate questions using AI based on candidate's parsed skills & projects
        List<InterviewQuestion> questions = generateQuestionsWithAi(candidate, profile, round);

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

    private List<InterviewQuestion> generateQuestionsWithAi(Candidate candidate, CandidateProfile profile, InterviewRound round) {
        String skills = profile.getSkills() != null ? profile.getSkills() : "None";
        StringBuilder projectsBuilder = new StringBuilder();
        if (profile.getProjects() != null && !profile.getProjects().isEmpty()) {
            for (CandidateProject p : profile.getProjects()) {
                projectsBuilder.append("- Project Name: ").append(p.getProjectName()).append("\n");
                projectsBuilder.append("  Tech Stack: ").append(p.getTechStack() != null ? p.getTechStack() : "None").append("\n");
                projectsBuilder.append("  Description: ").append(p.getDescription() != null ? p.getDescription() : "None").append("\n\n");
            }
        } else {
            projectsBuilder.append("None");
        }

        String userPrompt = String.format("Candidate Skills: %s\nCandidate Projects:\n%s", skills, projectsBuilder.toString());

        String systemPrompt = "You are an expert technical interviewer. Generate exactly 6 technical interview questions tailored to the candidate's skills and projects. " +
                "The questions must be categorized as: exactly 2 EASY, 2 MEDIUM, and 2 HARD questions. " +
                "For each question, provide:\n" +
                "1. questionText: The actual question.\n" +
                "2. difficultyLevel: The level of the question, must be exactly \"EASY\", \"MEDIUM\", or \"HARD\".\n" +
                "3. keyPoints: A list of 3 to 5 brief bullet points or phrases that represent the expected answers or key criteria the candidate should cover in their answer. " +
                "Return the results in the requested JSON structure.";

        try {
            QuestionGenerationResult result = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(QuestionGenerationResult.class);

            if (result == null || result.questions() == null || result.questions().isEmpty()) {
                throw new IllegalStateException("AI generated empty questions list");
            }

            List<InterviewQuestion> questions = new ArrayList<>();
            for (QuestionGenerationResult.AiQuestion aiQ : result.questions()) {
                DifficultyLevel level;
                try {
                    level = DifficultyLevel.valueOf(aiQ.difficultyLevel().toUpperCase().trim());
                } catch (Exception e) {
                    level = DifficultyLevel.MEDIUM; // Fallback
                }

                questions.add(buildQuestion(candidate, round, level, aiQ.questionText(), aiQ.keyPoints()));
            }
            return questions;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate questions via AI: " + e.getMessage(), e);
        }
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
