package com.p99softtraining.hiresense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "evaluation_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_eval_result_candidate_drive",
                        columnNames = {"candidate_id", "hiring_drive_id"}
                )
        }
)
public class EvaluationResult extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hiring_drive_id", nullable = false)
    private HiringDrive hiringDrive;

    @Column(nullable = false)
    private int totalScore = 0;
}
