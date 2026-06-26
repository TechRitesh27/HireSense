CREATE TABLE IF NOT EXISTS hiring_drives (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    CONSTRAINT fk_hiring_drives_company
        FOREIGN KEY (company_id)
        REFERENCES companies (id)
);

CREATE INDEX IF NOT EXISTS idx_hiring_drives_company_id
    ON hiring_drives (company_id);
