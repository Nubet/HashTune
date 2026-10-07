ALTER TABLE recognitions ADD COLUMN session_id VARCHAR(128);

CREATE INDEX idx_recognitions_session_created_at
    ON recognitions (session_id, created_at DESC);
