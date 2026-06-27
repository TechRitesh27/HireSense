CREATE TABLE IF NOT EXISTS interviewer_assignments (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    hiring_drive_id UUID NOT NULL,
    interviewer_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    CONSTRAINT fk_interviewer_assignments_hiring_drive
        FOREIGN KEY (hiring_drive_id)
        REFERENCES hiring_drives (id),
    CONSTRAINT fk_interviewer_assignments_interviewer
        FOREIGN KEY (interviewer_id)
        REFERENCES users (id),
    CONSTRAINT fk_interviewer_assignments_candidate
        FOREIGN KEY (candidate_id)
        REFERENCES candidates (id),
    CONSTRAINT uk_interviewer_assignment
        UNIQUE (hiring_drive_id, interviewer_id, candidate_id)
);

CREATE INDEX IF NOT EXISTS idx_interviewer_assignments_interviewer_id
    ON interviewer_assignments (interviewer_id);

CREATE INDEX IF NOT EXISTS idx_interviewer_assignments_candidate_id
    ON interviewer_assignments (candidate_id);

CREATE INDEX IF NOT EXISTS idx_interviewer_assignments_hiring_drive_id
    ON interviewer_assignments (hiring_drive_id);
