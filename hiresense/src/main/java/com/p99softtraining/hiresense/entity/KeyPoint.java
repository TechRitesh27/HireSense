package com.p99softtraining.hiresense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "key_points")
public class KeyPoint extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_question_id", nullable = false)
    private InterviewQuestion interviewQuestion;

    @Column(nullable = false, length = 1000)
    private String pointText;

    @Column(nullable = false)
    private boolean covered = false;
}
