package com.p99softtraining.hiresense.entity;

import com.p99softtraining.hiresense.enums.DifficultyLevel;
import com.p99softtraining.hiresense.enums.QuestionSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "interview_questions")
public class InterviewQuestion extends BaseEntity {

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id")
    private InterviewSession interviewSession;

    @Column(nullable = false, length = 2000)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficultyLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionSource source;

    @Column(length = 255)
    private String skill;
}
