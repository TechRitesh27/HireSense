-- Interview Rounds
CREATE TABLE IF NOT EXISTS interview_rounds (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    hiring_drive_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    round_type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_interview_rounds_hiring_drive
        FOREIGN KEY (hiring_drive_id)
        REFERENCES hiring_drives (id),
    CONSTRAINT uk_interview_round_hiring_drive_name
        UNIQUE (hiring_drive_id, name)
);

CREATE INDEX IF NOT EXISTS idx_interview_rounds_hiring_drive_id
    ON interview_rounds (hiring_drive_id);

-- Candidate Profiles
CREATE TABLE IF NOT EXISTS candidate_profiles (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    candidate_id UUID NOT NULL UNIQUE,
    skills VARCHAR(2000),
    status VARCHAR(50) NOT NULL,
    parsed_at TIMESTAMP,
    CONSTRAINT fk_candidate_profiles_candidate
        FOREIGN KEY (candidate_id)
        REFERENCES candidates (id)
);

-- Candidate Projects
CREATE TABLE IF NOT EXISTS candidate_projects (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    candidate_profile_id UUID NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    tech_stack VARCHAR(1000),
    description VARCHAR(2000),
    CONSTRAINT fk_candidate_projects_profile
        FOREIGN KEY (candidate_profile_id)
        REFERENCES candidate_profiles (id)
);

CREATE INDEX IF NOT EXISTS idx_candidate_projects_profile_id
    ON candidate_projects (candidate_profile_id);
