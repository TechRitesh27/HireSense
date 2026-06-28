-- Add is_asked flag to interview_questions
-- Only questions marked as asked are included in scoring
ALTER TABLE interview_questions ADD COLUMN IF NOT EXISTS is_asked BOOLEAN NOT NULL DEFAULT FALSE;

-- Add source flag to distinguish system-generated vs interviewer-added questions
ALTER TABLE interview_questions ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'GENERATED';
