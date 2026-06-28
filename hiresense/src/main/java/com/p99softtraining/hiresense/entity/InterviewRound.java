package com.p99softtraining.hiresense.entity;

import com.p99softtraining.hiresense.enums.RoundType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "interview_rounds",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_interview_round_hiring_drive_name",
                        columnNames = {"hiring_drive_id", "name"}
                )
        }
)
public class InterviewRound extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hiring_drive_id", nullable = false)
    private HiringDrive hiringDrive;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoundType roundType;
}
