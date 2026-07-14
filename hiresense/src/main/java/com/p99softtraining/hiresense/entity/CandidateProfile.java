package com.p99softtraining.hiresense.entity;

import com.p99softtraining.hiresense.enums.ProfileStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private Candidate candidate;

    // Stored as comma-separated values for simplicity (demo phase)
    @Column(length = 2000)
    private String primarySkills;

    @Column(length = 2000)
    private String secondarySkills;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProfileStatus status;

    private LocalDateTime parsedAt;

    // One profile has many projects
    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateProject> projects;
}
