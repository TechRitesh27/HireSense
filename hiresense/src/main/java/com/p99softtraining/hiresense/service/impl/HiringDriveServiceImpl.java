package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.config.CacheConfig;
import com.p99softtraining.hiresense.dto.request.CreateHiringDriveRequest;
import com.p99softtraining.hiresense.dto.request.UpdateHiringDriveStatusRequest;
import com.p99softtraining.hiresense.dto.response.HiringDriveResponse;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.entity.User;
import com.p99softtraining.hiresense.enums.HiringDriveStatus;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.HiringDriveRepository;
import com.p99softtraining.hiresense.repository.UserRepository;
import com.p99softtraining.hiresense.service.HiringDriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HiringDriveServiceImpl implements HiringDriveService {

    private final HiringDriveRepository hiringDriveRepository;
    private final UserRepository userRepository;

    @Override
    @CacheEvict(value = CacheConfig.HIRING_DRIVES, allEntries = true)
    public HiringDriveResponse createHiringDrive(CreateHiringDriveRequest request) {

        Company company = getCurrentUserCompany();
        validateDateRange(request);

        HiringDrive hiringDrive = new HiringDrive();
        hiringDrive.setCompany(company);
        hiringDrive.setTitle(request.getTitle());
        hiringDrive.setDescription(request.getDescription());
        hiringDrive.setStatus(HiringDriveStatus.DRAFT);
        hiringDrive.setStartDate(request.getStartDate());
        hiringDrive.setEndDate(request.getEndDate());

        return mapHiringDrive(hiringDriveRepository.save(hiringDrive));
    }

    @Override
    @Cacheable(value = CacheConfig.HIRING_DRIVES, key = "#root.methodName + '_' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public List<HiringDriveResponse> getHiringDrivesForCurrentCompany() {

        Company company = getCurrentUserCompany();

        return hiringDriveRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId())
                .stream()
                .map(this::mapHiringDrive)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.HIRING_DRIVES, allEntries = true)
    public HiringDriveResponse updateStatus(UUID hiringDriveId, UpdateHiringDriveStatusRequest request) {

        Company company = getCurrentUserCompany();

        HiringDrive hiringDrive = hiringDriveRepository.findById(hiringDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hiring drive not found"));

        if (!hiringDrive.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("Hiring drive not found");
        }

        hiringDrive.setStatus(request.getStatus());
        return mapHiringDrive(hiringDriveRepository.save(hiringDrive));
    }

    private void validateDateRange(CreateHiringDriveRequest request) {

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
    }

    private Company getCurrentUserCompany() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        if (user.getCompany() == null) {
            throw new IllegalArgumentException("Current user is not assigned to a company");
        }

        return user.getCompany();
    }

    private HiringDriveResponse mapHiringDrive(HiringDrive hiringDrive) {

        Company company = hiringDrive.getCompany();

        return HiringDriveResponse.builder()
                .id(hiringDrive.getId())
                .companyId(company.getId())
                .companyName(company.getName())
                .title(hiringDrive.getTitle())
                .description(hiringDrive.getDescription())
                .status(hiringDrive.getStatus())
                .startDate(hiringDrive.getStartDate())
                .endDate(hiringDrive.getEndDate())
                .build();
    }
}
