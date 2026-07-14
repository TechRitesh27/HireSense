package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.response.RankedCandidateResponse;

import java.util.List;
import java.util.UUID;

public interface RankingService {

    /** Returns candidates in a hiring drive round ranked by round score (desc) */
    List<RankedCandidateResponse> getRankedResults(UUID hiringDriveId, UUID roundId);
}
