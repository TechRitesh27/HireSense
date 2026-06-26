CREATE TABLE IF NOT EXISTS candidates (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    hiring_drive_id UUID NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    college_name VARCHAR(255) NOT NULL,
    degree VARCHAR(255) NOT NULL,
    branch VARCHAR(255) NOT NULL,
    graduation_year INTEGER NOT NULL,
    resume_url VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_candidates_hiring_drive
        FOREIGN KEY (hiring_drive_id)
        REFERENCES hiring_drives (id),
    CONSTRAINT uk_candidates_hiring_drive_email
        UNIQUE (hiring_drive_id, email)
);

CREATE INDEX IF NOT EXISTS idx_candidates_hiring_drive_id
    ON candidates (hiring_drive_id);
