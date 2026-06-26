package com.p99softtraining.hiresense.entity;

import com.p99softtraining.hiresense.enums.CandidateStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "candidates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_candidates_hiring_drive_email",
                        columnNames = {"hiring_drive_id", "email"}
                )
        }
)

public class Candidate extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "hiring_drive_id", nullable = false)
    private HiringDrive hiringDrive;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false)
    private String collegeName;

    @Column(nullable = false)
    private String degree;

    @Column(nullable = false)
    private String branch;

    @Column(nullable = false)
    private Integer graduationYear;

    @Column(nullable = false, length = 1000)
    private String resumeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CandidateStatus status;
}
