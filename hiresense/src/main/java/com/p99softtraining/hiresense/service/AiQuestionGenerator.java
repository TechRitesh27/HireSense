package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.GeneratedQuestion;
import com.p99softtraining.hiresense.enums.DifficultyLevel;
import com.p99softtraining.hiresense.exception.AiQuestionGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calls the configured LLM via Spring AI's {@link ChatClient} to generate a set of
 * interview questions tailored to a candidate's skills and a requested difficulty level.
 *
 * <p>Generation is retried once when the LLM returns a null or empty list.
 * After the retry the method throws {@link AiQuestionGenerationException} if the result
 * is still unusable.  Entries with a blank {@code questionText} or a {@code null}
 * {@code difficultyLevel} are filtered out before the list is returned.
 */
@Service
@RequiredArgsConstructor
public class AiQuestionGenerator {

    /**
     * System prompt sent to the LLM on every generation request.
     * Kept package-private so unit tests can assert on its content.
     */
    static final String SYSTEM_PROMPT =
            "You are an interview question generator. Given primary skills, secondary skills, " +
            "difficulty level, and a count, generate exactly {count} interview questions. " +
            "Allocate ~80% of questions to primary skills and ~20% to secondary skills. " +
            "Difficulty: EASY=conceptual/definitions, MEDIUM=applied/problem-solving, HARD=deep/system-design. " +
            "Return a JSON array: [{\"questionText\": \"...\", \"skill\": \"...\", \"difficultyLevel\": \"EASY|MEDIUM|HARD\"}]. " +
            "Every element must have a non-blank questionText and a valid difficultyLevel.";
    private final ChatClient chatClient;

    /**
     * Generates interview questions for a candidate session.
     *
     * @param primarySkills   comma-separated primary (role-specific) skills
     * @param secondarySkills comma-separated secondary skills (may be empty)
     * @param difficulty      the requested difficulty level
     * @param count           the number of questions to generate (≥ 1)
     * @return a non-null, possibly-empty list of {@link GeneratedQuestion} objects
     * @throws AiQuestionGenerationException if the LLM fails or returns unusable output after retry
     */
    public List<GeneratedQuestion> generate(
            List<String> primarySkills,
            List<String> secondarySkills,
            DifficultyLevel difficulty,
            int count
    ) {
        String userPrompt = buildUserPrompt(primarySkills, secondarySkills, difficulty, count);

        List<GeneratedQuestion> result = callLlm(userPrompt);

        if (isNullOrEmpty(result)) {
            // Retry once before giving up
            result = callLlm(userPrompt);
            if (isNullOrEmpty(result)) {
                throw new AiQuestionGenerationException(
                        "AI question generation returned an empty result after retry. " +
                        "The LLM did not produce any questions.");
            }
        }

        return filter(result);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildUserPrompt(
            List<String> primarySkills,
            List<String> secondarySkills,
            DifficultyLevel difficulty,
            int count
    ) {
        String primary = primarySkills == null || primarySkills.isEmpty()
                ? "(none)"
                : String.join(", ", primarySkills);

        String secondary = secondarySkills == null || secondarySkills.isEmpty()
                ? "(none)"
                : String.join(", ", secondarySkills);

        return String.format(
                "Primary skills: %s%n" +
                "Secondary skills: %s%n" +
                "Difficulty: %s%n" +
                "Count: %d",
                primary, secondary, difficulty.name(), count
        );
    }

    private List<GeneratedQuestion> callLlm(String userPrompt) {
        try {
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .entity(new ParameterizedTypeReference<List<GeneratedQuestion>>() {});
        } catch (AiQuestionGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new AiQuestionGenerationException(
                    "AI question generation failed: " + e.getMessage(), e);
        }
    }

    private boolean isNullOrEmpty(List<GeneratedQuestion> questions) {
        return questions == null || questions.isEmpty();
    }

    /**
     * Removes entries that cannot be used:
     * <ul>
     *   <li>blank or null {@code questionText}</li>
     *   <li>null {@code difficultyLevel}</li>
     * </ul>
     */
    private List<GeneratedQuestion> filter(List<GeneratedQuestion> questions) {
        return questions.stream()
                .filter(q -> q != null
                        && q.questionText() != null && !q.questionText().isBlank()
                        && q.difficultyLevel() != null)
                .toList();
    }
}
