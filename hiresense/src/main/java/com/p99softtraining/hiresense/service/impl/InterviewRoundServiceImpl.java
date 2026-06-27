package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.dto.request.CreateInterviewRoundRequest;
import com.p99softtraining.hiresense.dto.response.InterviewRoundResponse;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.entity.InterviewRound;
import com.p99softtraining.hiresense.enums.HiringDriveStatus;
import com.p99softtraining.hiresense.exception.ResourceAlreadyExistsException;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.HiringDriveRepository;
import com.p99softtraining.hiresense.repository.InterviewRoundRepository;
import com.p99softtraining.hiresense.service.InterviewRoundService;
import com.p99softtraining.hiresense.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewRoundServiceImpl implements InterviewRoundService {

    private final InterviewRoundRepository roundRepository;
    private final HiringDriveRepository hiringDriveRepository;
    private final SecurityService securityService;

    @Override
    @Transactional
    public InterviewRoundResponse createRound(UUID hiringDriveId, CreateInterviewRoundRequest request) {
        Company company = securityService.getCurrentUserCompany();
        HiringDrive hiringDrive = resolveCompanyHiringDrive(hiringDriveId, company.getId());

        if (hiringDrive.getStatus() != HiringDriveStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Rounds can only be created for ACTIVE hiring drives. Current status: " + hiringDrive.getStatus());
        }

        if (roundRepository.existsByHiringDriveIdAndName(hiringDriveId, request.getName())) {
            throw new ResourceAlreadyExistsException(
                    "A round with name '" + request.getName() + "' already exists in this hiring drive");
        }

        InterviewRound round = new InterviewRound();
        round.setHiringDrive(hiringDrive);
        round.setName(request.getName());
        round.setRoundType(request.getRoundType());

        return toResponse(roundRepository.save(round));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewRoundResponse> getRoundsForHiringDrive(UUID hiringDriveId) {
        Company company = securityService.getCurrentUserCompany();
        resolveCompanyHiringDrive(hiringDriveId, company.getId());

        return roundRepository.findByHiringDriveIdOrderByCreatedAtAsc(hiringDriveId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private HiringDrive resolveCompanyHiringDrive(UUID hiringDriveId, UUID companyId) {
        HiringDrive hiringDrive = hiringDriveRepository.findById(hiringDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hiring drive not found"));
        if (!hiringDrive.getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Hiring drive not found");
        }
        return hiringDrive;
    }

    private InterviewRoundResponse toResponse(InterviewRound round) {
        return InterviewRoundResponse.builder()
                .id(round.getId())
                .hiringDriveId(round.getHiringDrive().getId())
                .name(round.getName())
                .roundType(round.getRoundType())
                .build();
    }
}
