DELETE FROM indexing_jobs duplicate
USING indexing_jobs keeper
WHERE duplicate.track_id = keeper.track_id
  AND (duplicate.created_at, duplicate.id) < (keeper.created_at, keeper.id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_indexing_jobs_track_id
    ON indexing_jobs(track_id);
