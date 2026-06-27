package com.p99softtraining.hiresense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "interviewer_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_interviewer_assignment",
                        columnNames = {"hiring_drive_id", "interviewer_id", "candidate_id"}
                )
        }
)
public class InterviewerAssignment extends BaseEntity{

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hiring_drive_id", nullable = false)
    private HiringDrive hiringDrive;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
}
