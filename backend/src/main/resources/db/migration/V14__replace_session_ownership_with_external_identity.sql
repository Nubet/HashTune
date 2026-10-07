ALTER TABLE recognitions
    ADD COLUMN owner_issuer VARCHAR(512) NOT NULL;

ALTER TABLE recognitions
    ADD COLUMN owner_subject VARCHAR(255) NOT NULL;

DROP INDEX IF EXISTS idx_recognitions_session_created_at;

ALTER TABLE recognitions
    DROP COLUMN session_id;

CREATE INDEX idx_recognitions_owner_created_at
    ON recognitions (owner_issuer, owner_subject, created_at DESC);
