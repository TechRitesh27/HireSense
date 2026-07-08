package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.ResumeExtractionResult;
import com.p99softtraining.hiresense.exception.AiExtractionException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiResumeExtractor {

    static final String SYSTEM_PROMPT =
            "Extract skills and projects from resume text. Return JSON with 'skills' array and 'projects' array " +
            "(each with 'name', 'techStack', 'description'). Do not include candidate name, email, or phone number.";

    private final ChatClient chatClient;

    public ResumeExtractionResult extract(String resumeText) {
        ResumeExtractionResult result = callLlm(resumeText);

        if (isEmpty(result)) {
            // Retry once on null/empty result
            result = callLlm(resumeText);
            if (isEmpty(result)) {
                throw new AiExtractionException(
                        "AI extraction returned an empty result after retry. " +
                        "The LLM did not return any skills or projects.");
            }
        }

        return filter(result);
    }

    private ResumeExtractionResult callLlm(String resumeText) {
        try {
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(resumeText)
                    .call()
                    .entity(ResumeExtractionResult.class);
        } catch (Exception e) {
            throw new AiExtractionException("AI extraction failed: " + e.getMessage(), e);
        }
    }

    private boolean isEmpty(ResumeExtractionResult result) {
        if (result == null) {
            return true;
        }
        boolean noSkills = result.skills() == null || result.skills().isEmpty();
        boolean noProjects = result.projects() == null || result.projects().isEmpty();
        return noSkills && noProjects;
    }

    private ResumeExtractionResult filter(ResumeExtractionResult result) {
        List<String> filteredSkills = result.skills() == null
                ? List.of()
                : result.skills().stream()
                        .filter(s -> s != null && !s.isBlank())
                        .toList();

        List<ResumeExtractionResult.ProjectData> filteredProjects = result.projects() == null
                ? List.of()
                : result.projects().stream()
                        .filter(p -> p != null && p.name() != null && !p.name().isBlank())
                        .toList();

        return new ResumeExtractionResult(filteredSkills, filteredProjects);
    }
}
