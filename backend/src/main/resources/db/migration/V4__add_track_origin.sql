ALTER TABLE tracks ADD COLUMN IF NOT EXISTS origin VARCHAR(32) NOT NULL DEFAULT 'PERSONAL';

CREATE INDEX IF NOT EXISTS idx_tracks_origin_created_at
    ON tracks (origin, created_at DESC);
