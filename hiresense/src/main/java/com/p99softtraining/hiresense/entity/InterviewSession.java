package com.p99softtraining.hiresense.entity;

import com.p99softtraining.hiresense.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "interview_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_interviewer_candidate_round",
                        columnNames = {"interviewer_id", "candidate_id", "interview_round_id"}
                )
        }
)
public class InterviewSession extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_round_id", nullable = false)
    private InterviewRound interviewRound;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionStatus status;

    private LocalDateTime completedAt;
}
