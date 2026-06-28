-- Interview Questions
CREATE TABLE IF NOT EXISTS interview_questions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    candidate_id UUID NOT NULL,
    interview_round_id UUID NOT NULL,
    question_text VARCHAR(2000) NOT NULL,
    difficulty_level VARCHAR(20) NOT NULL,
    CONSTRAINT fk_interview_questions_candidate
        FOREIGN KEY (candidate_id)
        REFERENCES candidates (id),
    CONSTRAINT fk_interview_questions_round
        FOREIGN KEY (interview_round_id)
        REFERENCES interview_rounds (id)
);

CREATE INDEX IF NOT EXISTS idx_interview_questions_candidate_round
    ON interview_questions (candidate_id, interview_round_id);

-- Key Points
CREATE TABLE IF NOT EXISTS key_points (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    interview_question_id UUID NOT NULL,
    point_text VARCHAR(1000) NOT NULL,
    covered BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_key_points_question
        FOREIGN KEY (interview_question_id)
        REFERENCES interview_questions (id)
);

CREATE INDEX IF NOT EXISTS idx_key_points_question_id
    ON key_points (interview_question_id);

-- Interview Sessions
CREATE TABLE IF NOT EXISTS interview_sessions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    interviewer_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    interview_round_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_interview_sessions_interviewer
        FOREIGN KEY (interviewer_id)
        REFERENCES users (id),
    CONSTRAINT fk_interview_sessions_candidate
        FOREIGN KEY (candidate_id)
        REFERENCES candidates (id),
    CONSTRAINT fk_interview_sessions_round
        FOREIGN KEY (interview_round_id)
        REFERENCES interview_rounds (id),
    CONSTRAINT uk_session_interviewer_candidate_round
        UNIQUE (interviewer_id, candidate_id, interview_round_id)
);

-- Session Question Evaluations
CREATE TABLE IF NOT EXISTS session_question_evals (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    interview_session_id UUID NOT NULL,
    interview_question_id UUID NOT NULL,
    evaluator_notes VARCHAR(3000),
    additional_score INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_session_evals_session
        FOREIGN KEY (interview_session_id)
        REFERENCES interview_sessions (id),
    CONSTRAINT fk_session_evals_question
        FOREIGN KEY (interview_question_id)
        REFERENCES interview_questions (id),
    CONSTRAINT uk_eval_session_question
        UNIQUE (interview_session_id, interview_question_id)
);

-- Evaluation Results
CREATE TABLE IF NOT EXISTS evaluation_results (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    candidate_id UUID NOT NULL,
    hiring_drive_id UUID NOT NULL,
    total_score INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_eval_results_candidate
        FOREIGN KEY (candidate_id)
        REFERENCES candidates (id),
    CONSTRAINT fk_eval_results_hiring_drive
        FOREIGN KEY (hiring_drive_id)
        REFERENCES hiring_drives (id),
    CONSTRAINT uk_eval_result_candidate_drive
        UNIQUE (candidate_id, hiring_drive_id)
);

CREATE INDEX IF NOT EXISTS idx_eval_results_hiring_drive
    ON evaluation_results (hiring_drive_id);
