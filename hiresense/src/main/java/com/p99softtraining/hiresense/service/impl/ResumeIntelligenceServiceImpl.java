package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.config.CacheConfig;
import com.p99softtraining.hiresense.dto.response.CandidateProfileResponse;
import com.p99softtraining.hiresense.dto.response.CandidateProjectResponse;
import com.p99softtraining.hiresense.entity.Candidate;
import com.p99softtraining.hiresense.entity.CandidateProfile;
import com.p99softtraining.hiresense.entity.CandidateProject;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.enums.ProfileStatus;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.CandidateProfileRepository;
import com.p99softtraining.hiresense.repository.CandidateRepository;
import com.p99softtraining.hiresense.service.ResumeIntelligenceService;
import com.p99softtraining.hiresense.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeIntelligenceServiceImpl implements ResumeIntelligenceService {

    private final CandidateRepository candidateRepository;
    private final CandidateProfileRepository profileRepository;
    private final SecurityService securityService;

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CANDIDATE_PROFILE, key = "#candidateId")
    public CandidateProfileResponse parseResume(UUID candidateId) {
        Company company = securityService.getCurrentUserCompany();
        Candidate candidate = resolveCompanyCandidate(candidateId, company.getId());

        // Remove existing profile if any (re-parse overwrites)
        profileRepository.findByCandidateId(candidateId)
                .ifPresent(profileRepository::delete);

        // HARDCODED DEMO DATA
        CandidateProfile profile = new CandidateProfile();
        profile.setCandidate(candidate);
        profile.setSkills("Java, Spring Boot, REST APIs, PostgreSQL, Docker, Git, JUnit, Maven");
        profile.setStatus(ProfileStatus.PARSED);
        profile.setParsedAt(LocalDateTime.now());

        List<CandidateProject> projects = new ArrayList<>();

        CandidateProject p1 = new CandidateProject();
        p1.setCandidateProfile(profile);
        p1.setProjectName("E-Commerce Platform");
        p1.setTechStack("Java, Spring Boot, PostgreSQL, Docker");
        p1.setDescription("Built a full-stack e-commerce system with product management, cart, and payment integration.");

        CandidateProject p2 = new CandidateProject();
        p2.setCandidateProfile(profile);
        p2.setProjectName("Real-Time Chat Application");
        p2.setTechStack("Java, WebSocket, Spring Security, Redis");
        p2.setDescription("Developed a real-time chat app with JWT auth, private messaging, and online presence tracking.");

        projects.add(p1);
        projects.add(p2);
        profile.setProjects(projects);

        CandidateProfile saved = profileRepository.save(profile);
        return toResponse(saved);
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
        List<String> skills = Arrays.stream(profile.getSkills().split(","))
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
