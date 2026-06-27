package com.p99softtraining.hiresense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores the interviewer's per-question evaluation within a session:
 * - which key points were marked covered
 * - evaluator notes
 * - additional numeric score (0–10)
 */
@Getter
@Setter
@Entity
@Table(
        name = "session_question_evals",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_eval_session_question",
                        columnNames = {"interview_session_id", "interview_question_id"}
                )
        }
)
public class SessionQuestionEval extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id", nullable = false)
    private InterviewSession interviewSession;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_question_id", nullable = false)
    private InterviewQuestion interviewQuestion;

    @Column(length = 3000)
    private String evaluatorNotes;

    // Additional score awarded by the interviewer (0–10)
    @Column(nullable = false)
    private int additionalScore = 0;
}
