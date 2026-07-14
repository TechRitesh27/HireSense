-- V14: Add interview_round_id to evaluation_results and widen total_score to DOUBLE PRECISION
-- interview_round_id enables per-round scoring (Requirement 7.2)
-- total_score changed to DOUBLE PRECISION to store 0-100 decimal round scores (Requirement 6.4)

ALTER TABLE evaluation_results
    ADD COLUMN interview_round_id UUID REFERENCES interview_rounds(id);

ALTER TABLE evaluation_results
    ALTER COLUMN total_score TYPE DOUBLE PRECISION
        USING total_score::DOUBLE PRECISION;
