package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.dto.response.RankedCandidateResponse;
import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.entity.EvaluationResult;
import com.p99softtraining.hiresense.entity.HiringDrive;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.EvaluationResultRepository;
import com.p99softtraining.hiresense.repository.HiringDriveRepository;
import com.p99softtraining.hiresense.service.RankingService;
import com.p99softtraining.hiresense.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final EvaluationResultRepository evaluationResultRepository;
    private final HiringDriveRepository hiringDriveRepository;
    private final SecurityService securityService;

    @Override
    @Transactional(readOnly = true)
    public List<RankedCandidateResponse> getRankedResults(UUID hiringDriveId, UUID roundId) {
        Company company = securityService.getCurrentUserCompany();

        HiringDrive hiringDrive = hiringDriveRepository.findById(hiringDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hiring drive not found"));

        if (!hiringDrive.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Access denied: hiring drive does not belong to your company");
        }

        List<EvaluationResult> results =
                evaluationResultRepository.findByHiringDriveIdAndInterviewRoundIdOrderByTotalScoreDesc(
                        hiringDriveId, roundId);

        // Apply standard competition ranking (1,2,2,4,...)
        List<RankedCandidateResponse> ranked = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) {
                Double prev = results.get(i - 1).getTotalScore();
                Double curr = results.get(i).getTotalScore();
                double prevScore = prev != null ? prev : 0.0;
                double currScore = curr != null ? curr : 0.0;
                if (currScore < prevScore) {
                    rank = i + 1;
                }
            }
            EvaluationResult r = results.get(i);
            ranked.add(RankedCandidateResponse.builder()
                    .rank(rank)
                    .candidateId(r.getCandidate().getId())
                    .fullName(r.getCandidate().getFullName())
                    .email(r.getCandidate().getEmail())
                    .roundScore(r.getTotalScore() != null ? r.getTotalScore() : 0.0)
                    .build());
        }
        return ranked;
    }
}
