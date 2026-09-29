CREATE TABLE IF NOT EXISTS tracks (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    album VARCHAR(255),
    duration_ms BIGINT,
    audio_object_key VARCHAR(1024) NOT NULL,
    checksum VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

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
    finished_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_indexing_jobs_status_created_at ON indexing_jobs(status, created_at);

CREATE TABLE IF NOT EXISTS recognitions (
    id UUID PRIMARY KEY,
    track_id UUID REFERENCES tracks(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL,
    confidence DOUBLE PRECISION,
    matched_at_ms BIGINT,
    source VARCHAR(32) NOT NULL,
    sample_duration_ms BIGINT,
    recognition_time_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS audio_fingerprints (
    id UUID PRIMARY KEY,
    hash BIGINT NOT NULL,
    track_id UUID NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    anchor_offset_ms INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audio_fingerprints_hash ON audio_fingerprints(hash);
CREATE INDEX IF NOT EXISTS idx_audio_fingerprints_track_id ON audio_fingerprints(track_id);
