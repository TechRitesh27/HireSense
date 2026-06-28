package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.response.CandidateProfileResponse;

import java.util.UUID;

public interface ResumeIntelligenceService {

    /** Triggers resume parsing (hardcoded demo data) and stores CandidateProfile */
    CandidateProfileResponse parseResume(UUID candidateId);

    /** Returns the stored CandidateProfile for a candidate */
    CandidateProfileResponse getProfile(UUID candidateId);
}
