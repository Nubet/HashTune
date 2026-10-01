CREATE TABLE IF NOT EXISTS indexing_jobs (
    id UUID PRIMARY KEY,
    track_id UUID NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    progress INTEGER NOT NULL,
    attempts INTEGER NOT NULL,
    error_code VARCHAR(255),
    error_message VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    next_attempt_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_indexing_jobs_status_created_at
    ON indexing_jobs(status, created_at);

CREATE INDEX IF NOT EXISTS idx_indexing_jobs_ready
    ON indexing_jobs(status, next_attempt_at, created_at);
