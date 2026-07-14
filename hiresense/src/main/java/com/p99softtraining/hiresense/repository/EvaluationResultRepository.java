package com.p99softtraining.hiresense.repository;

import com.p99softtraining.hiresense.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, UUID> {

    Optional<EvaluationResult> findByCandidateIdAndHiringDriveId(UUID candidateId, UUID hiringDriveId);

    Optional<EvaluationResult> findByCandidateIdAndHiringDriveIdAndInterviewRoundId(
            UUID candidateId, UUID hiringDriveId, UUID interviewRoundId);

    List<EvaluationResult> findByHiringDriveIdOrderByTotalScoreDesc(UUID hiringDriveId);

    List<EvaluationResult> findByHiringDriveIdAndInterviewRoundIdOrderByTotalScoreDesc(
            UUID hiringDriveId, UUID interviewRoundId);
}
