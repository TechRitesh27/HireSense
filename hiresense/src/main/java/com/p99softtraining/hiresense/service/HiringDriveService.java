package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.CreateHiringDriveRequest;
import com.p99softtraining.hiresense.dto.request.UpdateHiringDriveStatusRequest;
import com.p99softtraining.hiresense.dto.response.HiringDriveResponse;

import java.util.List;
import java.util.UUID;

public interface HiringDriveService {

    HiringDriveResponse createHiringDrive(CreateHiringDriveRequest request);

    List<HiringDriveResponse> getHiringDrivesForCurrentCompany();

    HiringDriveResponse updateStatus(UUID hiringDriveId, UpdateHiringDriveStatusRequest request);
}
