-- V9: Split candidate_profiles.skills into primary_skills and secondary_skills
-- Requirement 1.4: Remove the single skills column and replace with primarySkills and secondarySkills

ALTER TABLE candidate_profiles
    ADD COLUMN primary_skills VARCHAR(2000),
    ADD COLUMN secondary_skills VARCHAR(2000);

-- Migrate existing data: copy skills into primary_skills, leave secondary_skills empty
UPDATE candidate_profiles
SET primary_skills   = skills,
    secondary_skills = '';

ALTER TABLE candidate_profiles DROP COLUMN skills;
