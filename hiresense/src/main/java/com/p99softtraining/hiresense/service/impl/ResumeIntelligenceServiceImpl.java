package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.config.CacheConfig;
import com.p99softtraining.hiresense.dto.ResumeExtractionResult;
import com.p99softtraining.hiresense.dto.response.CandidateProfileResponse;
import com.p99softtraining.hiresense.dto.response.CandidateProjectResponse;
import com.p99softtraining.hiresense.entity.Candidate;
import com.p99softtraining.hiresense.entity.CandidateProfile;
import com.p99softtraining.hiresense.entity.CandidateProject;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.enums.ProfileStatus;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.exception.ResumeTextExtractionException;
import com.p99softtraining.hiresense.exception.UnsupportedResumeFormatException;
import com.p99softtraining.hiresense.repository.CandidateProfileRepository;
import com.p99softtraining.hiresense.repository.CandidateRepository;
import com.p99softtraining.hiresense.service.AiResumeExtractor;
import com.p99softtraining.hiresense.service.ResumeDownloader;
import com.p99softtraining.hiresense.service.ResumeIntelligenceService;
import com.p99softtraining.hiresense.service.ResumeTextExtractor;
import com.p99softtraining.hiresense.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeIntelligenceServiceImpl implements ResumeIntelligenceService {

    private final CandidateRepository candidateRepository;
    private final CandidateProfileRepository profileRepository;
    private final SecurityService securityService;
    private final List<ResumeDownloader> resumeDownloaders;
    private final List<ResumeTextExtractor> resumeTextExtractors;
    private final AiResumeExtractor aiResumeExtractor;

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CANDIDATE_PROFILE, key = "#candidateId")
    public CandidateProfileResponse parseResume(UUID candidateId) {
        Company company = securityService.getCurrentUserCompany();
        Candidate candidate = resolveCompanyCandidate(candidateId, company.getId());

        // Step 1: Validate resume URL
        String resumeUrl = candidate.getResumeUrl();
        if (resumeUrl == null || resumeUrl.isBlank()) {
            throw new IllegalArgumentException("Candidate resume URL is blank");
        }

        try {
            // Step 2: Select downloader (Strategy pattern)
            ResumeDownloader downloader = resumeDownloaders.stream()
                    .filter(d -> d.supports(resumeUrl))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No supported downloader found for URL: " + resumeUrl));

            // Step 3: Download resume bytes
            byte[] bytes = downloader.download(resumeUrl);

            // Step 4: Select text extractor (Strategy pattern)
            ResumeTextExtractor extractor = resumeTextExtractors.stream()
                    .filter(e -> e.supports(bytes))
                    .findFirst()
                    .orElseThrow(() -> new UnsupportedResumeFormatException(
                            "Unsupported resume format for URL: " + resumeUrl));

            // Step 5: Extract text
            String text = extractor.extract(bytes);

            // Step 6: Validate extracted text length
            if (text.strip().length() < 50) {
                throw new ResumeTextExtractionException(
                        "Extracted text is too short (< 50 characters) for URL: " + resumeUrl);
            }

            // Step 7: AI extraction
            ResumeExtractionResult result = aiResumeExtractor.extract(text);

            // Step 8: Delete existing profile
            profileRepository.findByCandidateId(candidateId).ifPresent(p -> {
                profileRepository.delete(p);
                profileRepository.flush(); // Prevent unique constraint violation
            });

            // Step 9: Build CandidateProfile
            CandidateProfile profile = new CandidateProfile();
            profile.setCandidate(candidate);
            
            String skillsStr = String.join(", ", result.skills());
            if (skillsStr.length() > 2000) {
                skillsStr = skillsStr.substring(0, 1997) + "...";
            }
            profile.setSkills(skillsStr);
            
            profile.setStatus(ProfileStatus.PARSED);
            profile.setParsedAt(LocalDateTime.now());

            // Step 10: Build CandidateProject list
            List<CandidateProject> projects = result.projects().stream()
                    .map(p -> {
                        CandidateProject project = new CandidateProject();
                        project.setCandidateProfile(profile);
                        
                        String name = p.name() != null ? p.name() : "Unnamed Project";
                        project.setProjectName(name.length() > 255 ? name.substring(0, 255) : name);
                        
                        String techStack = p.techStack() != null ? String.join(", ", p.techStack()) : "";
                        project.setTechStack(techStack.length() > 1000 ? techStack.substring(0, 1000) : techStack);
                        
                        String desc = p.description() != null ? p.description() : "";
                        project.setDescription(desc.length() > 2000 ? desc.substring(0, 2000) : desc);
                        
                        return project;
                    })
                    .collect(Collectors.toList());
            profile.setProjects(projects);

            // Step 11: Persist and return
            CandidateProfile saved = profileRepository.save(profile);
            return toResponse(saved);

        } catch (Exception ex) {
            // On any pipeline failure: persist a FAILED profile, then rethrow
            try {
                profileRepository.findByCandidateId(candidateId).ifPresent(p -> {
                    profileRepository.delete(p);
                    profileRepository.flush(); // Ensure old profile is deleted immediately
                });
                CandidateProfile failed = new CandidateProfile();
                failed.setCandidate(candidate);
                failed.setStatus(ProfileStatus.FAILED);
                failed.setProjects(List.of());
                profileRepository.save(failed);
            } catch (Exception saveEx) {
                // Suppress save failure — original exception is more important
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CANDIDATE_PROFILE, key = "#candidateId")
    public CandidateProfileResponse getProfile(UUID candidateId) {
        Company company = securityService.getCurrentUserCompany();
        resolveCompanyCandidate(candidateId, company.getId());

        CandidateProfile profile = profileRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No profile found for candidate. Trigger parse-resume first."));
        return toResponse(profile);
    }

    private Candidate resolveCompanyCandidate(UUID candidateId, UUID companyId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (!candidate.getHiringDrive().getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Candidate not found");
        }
        return candidate;
    }

    private CandidateProfileResponse toResponse(CandidateProfile profile) {
        List<String> skills = profile.getSkills() == null || profile.getSkills().isBlank()
                ? List.of()
                : Arrays.stream(profile.getSkills().split(","))
                        .map(String::trim)
                        .toList();

        List<CandidateProjectResponse> projects = profile.getProjects() == null
                ? List.of()
                : profile.getProjects().stream()
                        .map(p -> CandidateProjectResponse.builder()
                                .id(p.getId())
                                .projectName(p.getProjectName())
                                .techStack(p.getTechStack())
                                .description(p.getDescription())
                                .build())
                        .toList();

        return CandidateProfileResponse.builder()
                .id(profile.getId())
                .candidateId(profile.getCandidate().getId())
                .candidateFullName(profile.getCandidate().getFullName())
                .skills(skills)
                .projects(projects)
                .status(profile.getStatus())
                .parsedAt(profile.getParsedAt())
                .build();
    }
}
