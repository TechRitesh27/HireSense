package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.CreateInterviewRoundRequest;
import com.p99softtraining.hiresense.dto.response.InterviewRoundResponse;

import java.util.List;
import java.util.UUID;

public interface InterviewRoundService {

    InterviewRoundResponse createRound(UUID hiringDriveId, CreateInterviewRoundRequest request);

    List<InterviewRoundResponse> getRoundsForHiringDrive(UUID hiringDriveId);
}
