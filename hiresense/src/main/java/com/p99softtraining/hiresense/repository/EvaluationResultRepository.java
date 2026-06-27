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

    List<EvaluationResult> findByHiringDriveIdOrderByTotalScoreDesc(UUID hiringDriveId);
}
