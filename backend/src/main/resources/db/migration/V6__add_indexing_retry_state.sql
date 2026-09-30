ALTER TABLE indexing_jobs
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_indexing_jobs_ready
    ON indexing_jobs(status, next_attempt_at, created_at);
