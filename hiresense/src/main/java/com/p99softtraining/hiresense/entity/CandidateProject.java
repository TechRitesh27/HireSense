package com.p99softtraining.hiresense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "candidate_projects")
public class CandidateProject extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(nullable = false)
    private String projectName;

    // Stored as comma-separated tech stack entries (demo phase)
    @Column(length = 1000)
    private String techStack;

    @Column(length = 2000)
    private String description;
}
