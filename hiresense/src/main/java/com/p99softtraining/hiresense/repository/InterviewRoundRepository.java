package com.p99softtraining.hiresense.repository;

import com.p99softtraining.hiresense.entity.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {

    boolean existsByHiringDriveIdAndName(UUID hiringDriveId, String name);

    List<InterviewRound> findByHiringDriveIdOrderByCreatedAtAsc(UUID hiringDriveId);
}
