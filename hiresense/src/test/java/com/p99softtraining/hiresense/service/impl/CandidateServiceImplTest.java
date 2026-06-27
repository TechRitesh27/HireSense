package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.dto.request.CreateCandidateRequest;
import com.p99softtraining.hiresense.dto.response.CandidateResponse;
import com.p99softtraining.hiresense.entity.Candidate;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.exception.ResourceAlreadyExistsException;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.mapper.CandidateMapper;
import com.p99softtraining.hiresense.parser.CandidateFileParser;
import com.p99softtraining.hiresense.repository.CandidateRepository;
import com.p99softtraining.hiresense.repository.HiringDriveRepository;
import com.p99softtraining.hiresense.service.SecurityService;
import com.p99softtraining.hiresense.service.SpreadsheetDownloader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceImplTest {

    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private HiringDriveRepository hiringDriveRepository;
    @Mock
    private SecurityService securityService;
    @Mock
    private CandidateMapper candidateMapper;
    @Mock
    private CandidateFileParser fileParser;
    @Mock
    private SpreadsheetDownloader spreadsheetDownloader;

    private CandidateServiceImpl candidateService;

    @BeforeEach
    void setUp() {
        candidateService = new CandidateServiceImpl(
                candidateRepository,
                hiringDriveRepository,
                securityService,
                candidateMapper,
                List.of(fileParser),
                List.of(spreadsheetDownloader)
        );
    }

    @Test
    void testCreateCandidateSuccess() {
        UUID hiringDriveId = UUID.randomUUID();
        CreateCandidateRequest request = new CreateCandidateRequest();
        request.setEmail("test@example.com");

        Company company = new Company();
        UUID companyId = UUID.randomUUID();
        company.setId(companyId);

        HiringDrive hiringDrive = new HiringDrive();
        hiringDrive.setId(hiringDriveId);
        hiringDrive.setCompany(company);

        Candidate candidate = new Candidate();
        CandidateResponse response = CandidateResponse.builder().email("test@example.com").build();

        when(securityService.getCurrentUserCompany()).thenReturn(company);
        when(hiringDriveRepository.findById(hiringDriveId)).thenReturn(Optional.of(hiringDrive));
        when(candidateRepository.existsByHiringDriveIdAndEmail(hiringDriveId, request.getEmail())).thenReturn(false);
        when(candidateMapper.toEntity(request, hiringDrive)).thenReturn(candidate);
        when(candidateRepository.save(candidate)).thenReturn(candidate);
        when(candidateMapper.toResponse(candidate)).thenReturn(response);

        CandidateResponse actualResponse = candidateService.createCandidate(hiringDriveId, request);

        assertNotNull(actualResponse);
        assertEquals("test@example.com", actualResponse.getEmail());
        verify(candidateRepository, times(1)).save(candidate);
    }

    @Test
    void testCreateCandidateDuplicateEmailThrowsException() {
        UUID hiringDriveId = UUID.randomUUID();
        CreateCandidateRequest request = new CreateCandidateRequest();
        request.setEmail("test@example.com");

        Company company = new Company();
        company.setId(UUID.randomUUID());

        HiringDrive hiringDrive = new HiringDrive();
        hiringDrive.setId(hiringDriveId);
        hiringDrive.setCompany(company);

        when(securityService.getCurrentUserCompany()).thenReturn(company);
        when(hiringDriveRepository.findById(hiringDriveId)).thenReturn(Optional.of(hiringDrive));
        when(candidateRepository.existsByHiringDriveIdAndEmail(hiringDriveId, request.getEmail())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () ->
                candidateService.createCandidate(hiringDriveId, request)
        );
        verify(candidateRepository, never()).save(any());
    }

    @Test
    void testCreateCandidateAccessDeniedThrowsException() {
        UUID hiringDriveId = UUID.randomUUID();
        CreateCandidateRequest request = new CreateCandidateRequest();

        Company company = new Company();
        company.setId(UUID.randomUUID());

        Company otherCompany = new Company();
        otherCompany.setId(UUID.randomUUID());

        HiringDrive hiringDrive = new HiringDrive();
        hiringDrive.setId(hiringDriveId);
        hiringDrive.setCompany(otherCompany); // different company!

        when(securityService.getCurrentUserCompany()).thenReturn(company);
        when(hiringDriveRepository.findById(hiringDriveId)).thenReturn(Optional.of(hiringDrive));

        assertThrows(ResourceNotFoundException.class, () ->
                candidateService.createCandidate(hiringDriveId, request)
        );
        verify(candidateRepository, never()).save(any());
    }
}
