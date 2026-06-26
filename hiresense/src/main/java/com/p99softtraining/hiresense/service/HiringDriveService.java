package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.CreateHiringDriveRequest;
import com.p99softtraining.hiresense.dto.response.HiringDriveResponse;

import java.util.List;

public interface HiringDriveService {

    HiringDriveResponse createHiringDrive(CreateHiringDriveRequest request);

    List<HiringDriveResponse> getHiringDrivesForCurrentCompany();
}
