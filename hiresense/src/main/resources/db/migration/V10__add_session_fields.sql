-- V10: Add difficulty_level, question_count, and session_score to interview_sessions
-- Required for session start (Requirement 2.6) and score storage (Requirement 6.6)

ALTER TABLE interview_sessions
    ADD COLUMN difficulty_level VARCHAR(50),
    ADD COLUMN question_count   INTEGER,
    ADD COLUMN session_score    DOUBLE PRECISION;
