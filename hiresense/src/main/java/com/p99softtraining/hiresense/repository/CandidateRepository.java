package com.p99softtraining.hiresense.repository;

import com.p99softtraining.hiresense.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    boolean existsByHiringDriveIdAndEmail(UUID hiringDriveId, String email);

    List<Candidate> findByHiringDriveIdOrderByCreatedAtDesc(UUID hiringDriveId);
}
