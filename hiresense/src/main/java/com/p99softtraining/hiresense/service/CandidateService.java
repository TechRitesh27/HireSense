package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.CreateCandidateRequest;
import com.p99softtraining.hiresense.dto.response.CandidateResponse;
import com.p99softtraining.hiresense.dto.response.ExcelUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CandidateService {

    CandidateResponse createCandidate(UUID hiringDriveId, CreateCandidateRequest request);

    List<CandidateResponse> getCandidatesForHiringDrive(UUID hiringDriveId);

    ExcelUploadResponse uploadCandidatesFromExcel(UUID hiringDriveId, MultipartFile file);
}
