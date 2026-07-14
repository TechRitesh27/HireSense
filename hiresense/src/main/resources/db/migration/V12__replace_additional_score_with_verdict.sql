-- V12: Replace additional_score with verdict on session_question_evals
-- additionalScore (int 0-10) is replaced by verdict enum (GOOD/AVERAGE/POOR)
-- Requirement 10.3 (evaluation model update)

ALTER TABLE session_question_evals DROP COLUMN additional_score;

ALTER TABLE session_question_evals
    ADD COLUMN verdict VARCHAR(50);
