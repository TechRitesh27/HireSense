-- V11: Restructure interview_questions to be session-scoped
-- Drops candidate_id and interview_round_id (and their FKs), adds interview_session_id
-- Also adds skill column; source column already exists from V8 (alter to wider type)
-- Requirement 10.3 (KeyPoint/question restructure)

-- Drop the composite index that references columns being removed
DROP INDEX IF EXISTS idx_interview_questions_candidate_round;

-- Drop foreign key constraints added in V7
ALTER TABLE interview_questions DROP CONSTRAINT IF EXISTS fk_interview_questions_candidate;
ALTER TABLE interview_questions DROP CONSTRAINT IF EXISTS fk_interview_questions_round;

-- Remove the old scope columns
ALTER TABLE interview_questions DROP COLUMN IF EXISTS candidate_id;
ALTER TABLE interview_questions DROP COLUMN IF EXISTS interview_round_id;

-- Remove is_asked column added in V8 (superseded by session-scoped model)
ALTER TABLE interview_questions DROP COLUMN IF EXISTS is_asked;

-- Add session-scope FK column
ALTER TABLE interview_questions
    ADD COLUMN interview_session_id UUID REFERENCES interview_sessions(id);

-- Widen and normalize source column (was VARCHAR(20) DEFAULT 'GENERATED' from V8)
ALTER TABLE interview_questions ALTER COLUMN source TYPE VARCHAR(50);
ALTER TABLE interview_questions ALTER COLUMN source SET DEFAULT 'AI_GENERATED';
UPDATE interview_questions SET source = 'AI_GENERATED' WHERE source = 'GENERATED';

-- Add skill column
ALTER TABLE interview_questions
    ADD COLUMN skill VARCHAR(255);

-- Add index on the new session FK
CREATE INDEX IF NOT EXISTS idx_interview_questions_session_id
    ON interview_questions (interview_session_id);
