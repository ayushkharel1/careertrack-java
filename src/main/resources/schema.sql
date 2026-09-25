CREATE TABLE IF NOT EXISTS applications (
    id UUID PRIMARY KEY,
    company VARCHAR(100) NOT NULL,
    role VARCHAR(140) NOT NULL,
    location VARCHAR(120) NOT NULL DEFAULT '',
    stage VARCHAR(20) NOT NULL CHECK (stage IN ('SAVED', 'APPLIED', 'INTERVIEW', 'OFFER', 'CLOSED')),
    applied_on DATE,
    interview_on DATE,
    notes VARCHAR(5000) NOT NULL DEFAULT '',
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
