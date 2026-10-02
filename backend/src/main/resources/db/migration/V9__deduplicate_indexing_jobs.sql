DELETE FROM indexing_jobs
WHERE id IN (
    SELECT duplicate.id
    FROM indexing_jobs duplicate
    JOIN indexing_jobs keeper ON duplicate.track_id = keeper.track_id
    WHERE duplicate.created_at < keeper.created_at
       OR (duplicate.created_at = keeper.created_at AND duplicate.id < keeper.id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_indexing_jobs_track_id
    ON indexing_jobs(track_id);
