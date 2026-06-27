package com.p99softtraining.hiresense.repository;

import com.p99softtraining.hiresense.entity.InterviewerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewerAssignmentRepository extends JpaRepository<InterviewerAssignment, UUID> {

    boolean existsByHiringDriveIdAndInterviewerIdAndCandidateId(
            UUID hiringDriveId,
            UUID interviewerId,
            UUID candidateId
    );

    List<InterviewerAssignment> findByHiringDriveIdOrderByCreatedAtDesc(UUID hiringDriveId);

    List<InterviewerAssignment> findByInterviewerIdAndHiringDrive_Company_IdOrderByCreatedAtDesc(
            UUID interviewerId,
            UUID companyId
    );
}
