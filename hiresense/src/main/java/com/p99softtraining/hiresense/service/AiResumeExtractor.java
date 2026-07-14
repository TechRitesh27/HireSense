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
            "Extract skills and projects from resume text. " +
            "Return JSON with 'primarySkills' array (core/primary technical skills which match the candidate profile and seems as mostly matched to him), " +
            "'secondarySkills' array (secondary/supporting skills other than primary. Primary skills should be limited but highly related ), " +
            "and 'projects' array (each with 'name', 'techStack', 'description'). " +
            "Example: { \"primarySkills\": [...], \"secondarySkills\": [...], \"projects\": [...] }. " +
            "Do not include candidate name, email, or phone number. Also don't add any extra skills by your own.";
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
        boolean noPrimarySkills = result.primarySkills() == null || result.primarySkills().isEmpty();
        boolean noSecondarySkills = result.secondarySkills() == null || result.secondarySkills().isEmpty();
        boolean noProjects = result.projects() == null || result.projects().isEmpty();
        return noPrimarySkills && noSecondarySkills && noProjects;
    }

    private ResumeExtractionResult filter(ResumeExtractionResult result) {
        List<String> filteredPrimarySkills = result.primarySkills() == null
                ? List.of()
                : result.primarySkills().stream()
                        .filter(s -> s != null && !s.isBlank())
                        .toList();

        List<String> filteredSecondarySkills = result.secondarySkills() == null
                ? List.of()
                : result.secondarySkills().stream()
                        .filter(s -> s != null && !s.isBlank())
                        .toList();

        List<ResumeExtractionResult.ProjectData> filteredProjects = result.projects() == null
                ? List.of()
                : result.projects().stream()
                        .filter(p -> p != null && p.name() != null && !p.name().isBlank())
                        .toList();

        return new ResumeExtractionResult(filteredPrimarySkills, filteredSecondarySkills, filteredProjects);
    }
}
