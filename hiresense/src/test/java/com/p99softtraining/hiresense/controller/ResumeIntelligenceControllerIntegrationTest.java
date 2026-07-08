package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.config.CacheConfig;
import com.p99softtraining.hiresense.dto.ResumeExtractionResult;
import com.p99softtraining.hiresense.dto.response.CandidateProfileResponse;
import com.p99softtraining.hiresense.dto.response.CandidateProjectResponse;
import com.p99softtraining.hiresense.entity.Candidate;
import com.p99softtraining.hiresense.entity.CandidateProfile;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.enums.CandidateStatus;
import com.p99softtraining.hiresense.enums.HiringDriveStatus;
import com.p99softtraining.hiresense.enums.ProfileStatus;
import com.p99softtraining.hiresense.enums.Role;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.CandidateProfileRepository;
import com.p99softtraining.hiresense.repository.CandidateRepository;
import com.p99softtraining.hiresense.repository.UserRepository;
import com.p99softtraining.hiresense.service.AiResumeExtractor;
import com.p99softtraining.hiresense.service.ResumeDownloader;
import com.p99softtraining.hiresense.service.ResumeTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ResumeIntelligenceController}.
 * <p>
 * Uses the real {@code ResumeIntelligenceServiceImpl} so that {@code @Cacheable}/
 * {@code @CacheEvict} and role-based security are exercised end-to-end. All
 * external I/O is eliminated by mocking the pipeline components (downloaders,
 * extractors) and the repositories.
 * <p>
 * Tests cover:
 * <ul>
 *   <li>10.1 – POST /api/v1/candidates/{candidateId}/parse-resume</li>
 *   <li>10.2 – GET  /api/v1/candidates/{candidateId}/profile</li>
 *   <li>10.3 – Re-parse with existing profile and cache eviction</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.ai.google.genai.api-key=test-key-not-real"
})
class ResumeIntelligenceControllerIntegrationTest {

    // ─── Injected Beans ──────────────────────────────────────────────────────────

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    // ─── Mocked Pipeline Components (by name to resolve ambiguity) ───────────────

    @MockitoBean(name = "googleDriveResumeDownloader")
    private ResumeDownloader googleDriveDownloader;

    @MockitoBean(name = "oneDriveResumeDownloader")
    private ResumeDownloader oneDriveDownloader;

    @MockitoBean(name = "generalHttpResumeDownloader")
    private ResumeDownloader generalHttpDownloader;

    @MockitoBean(name = "pdfTextExtractor")
    private ResumeTextExtractor pdfTextExtractor;

    @MockitoBean(name = "docxTextExtractor")
    private ResumeTextExtractor docxTextExtractor;

    @MockitoBean
    private AiResumeExtractor aiResumeExtractor;

    // Spring AI ChatClient — mocked so auto-config doesn't fail
    @MockitoBean
    private ChatClient chatClient;

    // ─── Mocked Repositories ─────────────────────────────────────────────────────

    @MockitoBean
    private CandidateRepository candidateRepository;

    @MockitoBean
    private CandidateProfileRepository candidateProfileRepository;

    @MockitoBean
    private UserRepository userRepository;

    // ─── Test Data ────────────────────────────────────────────────────────────────

    private UUID companyId;
    private UUID candidateId;
    private Candidate candidate;
    private Company company;
    private CandidateProfile parsedProfile;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        Cache cache = cacheManager.getCache(CacheConfig.CANDIDATE_PROFILE);
        if (cache != null) {
            cache.clear();
        }
        Mockito.reset(googleDriveDownloader, oneDriveDownloader, generalHttpDownloader,
                pdfTextExtractor, docxTextExtractor, aiResumeExtractor,
                candidateRepository, candidateProfileRepository, userRepository);

        companyId = UUID.randomUUID();
        candidateId = UUID.randomUUID();

        company = new Company();
        company.setId(companyId);
        company.setName("Acme Corp");
        company.setAddress("123 Main St");
        company.setEmail("admin@acme.com");

        HiringDrive drive = new HiringDrive();
        drive.setId(UUID.randomUUID());
        drive.setCompany(company);
        drive.setTitle("SWE Drive 2025");
        drive.setStatus(HiringDriveStatus.ACTIVE);
        drive.setStartDate(LocalDate.now());
        drive.setEndDate(LocalDate.now().plusDays(30));

        candidate = new Candidate();
        candidate.setId(candidateId);
        candidate.setFullName("Alice Smith");
        candidate.setEmail("alice@example.com");
        candidate.setPhone("1234567890");
        candidate.setCollegeName("MIT");
        candidate.setDegree("B.Tech");
        candidate.setBranch("CS");
        candidate.setGraduationYear(2024);
        candidate.setResumeUrl("https://docs.google.com/file/d/abc123/view");
        candidate.setStatus(CandidateStatus.IMPORTED);
        candidate.setHiringDrive(drive);

        parsedProfile = new CandidateProfile();
        parsedProfile.setId(UUID.randomUUID());
        parsedProfile.setCandidate(candidate);
        parsedProfile.setSkills("Java, Spring Boot");
        parsedProfile.setStatus(ProfileStatus.PARSED);
        parsedProfile.setParsedAt(LocalDateTime.now());
        parsedProfile.setProjects(List.of());

        // Default: admin user belongs to the same company
        com.p99softtraining.hiresense.entity.User adminUser = buildUser("admin@acme.com", Role.COMPANY_ADMIN, company);
        when(userRepository.findByEmail("admin@acme.com")).thenReturn(Optional.of(adminUser));

        com.p99softtraining.hiresense.entity.User interviewerUser = buildUser("interviewer@acme.com", Role.INTERVIEWER, company);
        when(userRepository.findByEmail("interviewer@acme.com")).thenReturn(Optional.of(interviewerUser));
    }

    // ─── 10.1: POST /api/v1/candidates/{candidateId}/parse-resume ─────────────────

    /**
     * COMPANY_ADMIN can trigger parse → returns CandidateProfileResponse (200 OK).
     * Requirements: 5.1
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void parseResume_asCompanyAdmin_returnsProfileResponse() throws Exception {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateProfileRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());
        when(googleDriveDownloader.supports(any())).thenReturn(true);
        when(googleDriveDownloader.download(any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});
        when(pdfTextExtractor.supports(any())).thenReturn(true);
        when(pdfTextExtractor.extract(any())).thenReturn(
                "Alice Smith Java developer with 5 years of experience in Spring Boot."
        );
        when(aiResumeExtractor.extract(any())).thenReturn(
                new ResumeExtractionResult(List.of("Java", "Spring Boot"), List.of())
        );
        when(candidateProfileRepository.save(any())).thenReturn(parsedProfile);

        mockMvc.perform(post("/api/v1/candidates/{candidateId}/parse-resume", candidateId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value(candidateId.toString()))
                .andExpect(jsonPath("$.status").value("PARSED"));
    }

    /**
     * INTERVIEWER cannot trigger parse.
     * <p>
     * The {@code @PreAuthorize("hasRole('COMPANY_ADMIN')")} throws
     * {@code AuthorizationDeniedException} which the existing
     * {@code GlobalExceptionHandler.handleRuntimeException()} maps to HTTP 400.
     * Requirements: 5.3
     */
    @Test
    @WithMockUser(username = "interviewer@acme.com", roles = "INTERVIEWER")
    void parseResume_asInterviewer_returnsDenied() throws Exception {
        mockMvc.perform(post("/api/v1/candidates/{candidateId}/parse-resume", candidateId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());     // GlobalExceptionHandler maps RuntimeException → 400

        // The service should never be called for a denied request
        verify(candidateRepository, never()).findById(any());
    }

    /**
     * Candidate not found → returns HTTP 404.
     * Requirements: 5.2
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void parseResume_candidateNotFound_returnsNotFound() throws Exception {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/candidates/{candidateId}/parse-resume", candidateId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Cross-company candidate → returns HTTP 404.
     * Requirements: 5.2
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void parseResume_crossCompanyCandidate_returnsNotFound() throws Exception {
        Company otherCompany = new Company();
        otherCompany.setId(UUID.randomUUID());
        otherCompany.setName("Other Corp");
        otherCompany.setAddress("456 Other St");
        otherCompany.setEmail("admin@other.com");

        HiringDrive otherDrive = new HiringDrive();
        otherDrive.setId(UUID.randomUUID());
        otherDrive.setCompany(otherCompany);
        otherDrive.setTitle("Other Drive");
        otherDrive.setStatus(HiringDriveStatus.ACTIVE);
        otherDrive.setStartDate(LocalDate.now());
        otherDrive.setEndDate(LocalDate.now().plusDays(30));

        candidate.setHiringDrive(otherDrive);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        mockMvc.perform(post("/api/v1/candidates/{candidateId}/parse-resume", candidateId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Blank resumeUrl → returns HTTP 400 (mapped by GlobalExceptionHandler).
     * Requirements: 5.4
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void parseResume_blankResumeUrl_returnsBadRequest() throws Exception {
        candidate.setResumeUrl("");
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateProfileRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());
        when(candidateProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/candidates/{candidateId}/parse-resume", candidateId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ─── 10.2: GET /api/v1/candidates/{candidateId}/profile ──────────────────────

    /**
     * COMPANY_ADMIN can retrieve profile → 200 OK.
     * Requirements: 6.1
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void getProfile_asCompanyAdmin_returnsProfileResponse() throws Exception {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateProfileRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(parsedProfile));

        mockMvc.perform(get("/api/v1/candidates/{candidateId}/profile", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value(candidateId.toString()))
                .andExpect(jsonPath("$.status").value("PARSED"));
    }

    /**
     * INTERVIEWER can retrieve profile → 200 OK.
     * Requirements: 6.1
     */
    @Test
    @WithMockUser(username = "interviewer@acme.com", roles = "INTERVIEWER")
    void getProfile_asInterviewer_returnsProfileResponse() throws Exception {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateProfileRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(parsedProfile));

        mockMvc.perform(get("/api/v1/candidates/{candidateId}/profile", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARSED"));
    }

    /**
     * No profile exists → HTTP 404.
     * Requirements: 6.3
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void getProfile_noProfileExists_returnsNotFound() throws Exception {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateProfileRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/candidates/{candidateId}/profile", candidateId))
                .andExpect(status().isNotFound());
    }

    /**
     * Cross-company candidate → HTTP 404.
     * Requirements: 6.2
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void getProfile_crossCompanyCandidate_returnsNotFound() throws Exception {
        Company otherCompany = new Company();
        otherCompany.setId(UUID.randomUUID());
        otherCompany.setName("Other Corp");
        otherCompany.setAddress("789 Other Ave");
        otherCompany.setEmail("info@other.com");

        HiringDrive otherDrive = new HiringDrive();
        otherDrive.setId(UUID.randomUUID());
        otherDrive.setCompany(otherCompany);
        otherDrive.setTitle("Other Drive");
        otherDrive.setStatus(HiringDriveStatus.ACTIVE);
        otherDrive.setStartDate(LocalDate.now());
        otherDrive.setEndDate(LocalDate.now().plusDays(30));

        candidate.setHiringDrive(otherDrive);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        mockMvc.perform(get("/api/v1/candidates/{candidateId}/profile", candidateId))
                .andExpect(status().isNotFound());
    }

    /**
     * Cache hit: a second call to getProfile() for the same candidateId should be
     * served from the Spring cache — the repository is only queried once.
     * Requirements: 6.4
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void getProfile_cacheHit_noDuplicateDbQuery() throws Exception {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateProfileRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(parsedProfile));

        // First call — populates cache
        mockMvc.perform(get("/api/v1/candidates/{candidateId}/profile", candidateId))
                .andExpect(status().isOk());

        // Second call — served from cache
        mockMvc.perform(get("/api/v1/candidates/{candidateId}/profile", candidateId))
                .andExpect(status().isOk());

        // Repository should only be queried once; second call is served from cache
        verify(candidateProfileRepository, times(1)).findByCandidateId(candidateId);
    }

    // ─── 10.3: Re-parse with existing profile ────────────────────────────────────

    /**
     * Re-parse candidate with PARSED status → succeeds, overwrites previous profile.
     * Requirements: 5.6
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void parseResume_candidateWithParsedStatus_overwritesPreviousProfile() throws Exception {
        CandidateProfile existing = buildCandidateProfile(candidate, ProfileStatus.PARSED);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateProfileRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(existing));
        when(googleDriveDownloader.supports(any())).thenReturn(true);
        when(googleDriveDownloader.download(any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});
        when(pdfTextExtractor.supports(any())).thenReturn(true);
        when(pdfTextExtractor.extract(any())).thenReturn(
                "Alice Smith Python developer with updated skills. More than fifty characters here."
        );
        when(aiResumeExtractor.extract(any())).thenReturn(
                new ResumeExtractionResult(List.of("Python", "FastAPI"), List.of())
        );
        CandidateProfile newProfile = buildCandidateProfile(candidate, ProfileStatus.PARSED);
        when(candidateProfileRepository.save(any())).thenReturn(newProfile);

        mockMvc.perform(post("/api/v1/candidates/{candidateId}/parse-resume", candidateId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARSED"));

        // The old profile must be deleted before the new one is saved
        verify(candidateProfileRepository).delete(existing);
        verify(candidateProfileRepository).save(any(CandidateProfile.class));
    }

    /**
     * Re-parse candidate with FAILED status → succeeds, overwrites previous profile.
     * Requirements: 5.6
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void parseResume_candidateWithFailedStatus_overwritesPreviousProfile() throws Exception {
        CandidateProfile failedProfile = buildCandidateProfile(candidate, ProfileStatus.FAILED);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateProfileRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(failedProfile));
        when(googleDriveDownloader.supports(any())).thenReturn(true);
        when(googleDriveDownloader.download(any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});
        when(pdfTextExtractor.supports(any())).thenReturn(true);
        when(pdfTextExtractor.extract(any())).thenReturn(
                "Alice Smith Go developer ready for re-parse after failure."
        );
        when(aiResumeExtractor.extract(any())).thenReturn(
                new ResumeExtractionResult(List.of("Go", "Docker"), List.of())
        );
        CandidateProfile newProfile = buildCandidateProfile(candidate, ProfileStatus.PARSED);
        when(candidateProfileRepository.save(any())).thenReturn(newProfile);

        mockMvc.perform(post("/api/v1/candidates/{candidateId}/parse-resume", candidateId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARSED"));

        // The failed profile must be deleted before the new one is saved
        verify(candidateProfileRepository).delete(failedProfile);
        verify(candidateProfileRepository).save(any(CandidateProfile.class));
    }

    /**
     * After re-parse, the cache entry for this candidateId should be evicted so
     * the next getProfile() call fetches fresh data.
     * Requirements: 4.7
     */
    @Test
    @WithMockUser(username = "admin@acme.com", roles = "COMPANY_ADMIN")
    void parseResume_evictsCacheAfterReparse() throws Exception {
        // Pre-populate the cache (simulates a prior getProfile() call)
        Cache cache = cacheManager.getCache(CacheConfig.CANDIDATE_PROFILE);
        if (cache != null) {
            cache.put(candidateId, parsedProfile);
        }

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateProfileRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());
        when(googleDriveDownloader.supports(any())).thenReturn(true);
        when(googleDriveDownloader.download(any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});
        when(pdfTextExtractor.supports(any())).thenReturn(true);
        when(pdfTextExtractor.extract(any())).thenReturn(
                "Alice Smith Kotlin developer for cache eviction test."
        );
        when(aiResumeExtractor.extract(any())).thenReturn(
                new ResumeExtractionResult(List.of("Kotlin"), List.of())
        );
        CandidateProfile newProfile = buildCandidateProfile(candidate, ProfileStatus.PARSED);
        when(candidateProfileRepository.save(any())).thenReturn(newProfile);

        mockMvc.perform(post("/api/v1/candidates/{candidateId}/parse-resume", candidateId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // @CacheEvict on parseResume() should have cleared the entry
        Cache.ValueWrapper cachedValue = cache != null ? cache.get(candidateId) : null;
        assertNull(cachedValue, "Cache entry should have been evicted after parse-resume");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private com.p99softtraining.hiresense.entity.User buildUser(String email, Role role, Company company) {
        com.p99softtraining.hiresense.entity.User user = new com.p99softtraining.hiresense.entity.User();
        user.setId(UUID.randomUUID());
        user.setFullName("Test User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setCompany(company);
        return user;
    }

    private CandidateProfile buildCandidateProfile(Candidate candidate, ProfileStatus status) {
        CandidateProfile profile = new CandidateProfile();
        profile.setId(UUID.randomUUID());
        profile.setCandidate(candidate);
        profile.setSkills("Java, Spring Boot");
        profile.setStatus(status);
        profile.setProjects(List.of());
        if (status == ProfileStatus.PARSED) {
            profile.setParsedAt(LocalDateTime.now());
        }
        return profile;
    }
}
