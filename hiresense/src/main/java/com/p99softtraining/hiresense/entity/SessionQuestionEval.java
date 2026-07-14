package com.p99softtraining.hiresense.entity;

import com.p99softtraining.hiresense.enums.Verdict;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores the interviewer's per-question evaluation within a session:
 * - evaluator notes
 * - verdict (GOOD, AVERAGE, POOR)
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

    @Column(name = "evaluator_notes", length = 3000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Verdict verdict;
}
