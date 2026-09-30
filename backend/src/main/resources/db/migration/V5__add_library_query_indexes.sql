CREATE INDEX IF NOT EXISTS idx_tracks_origin_title_artist_id
    ON tracks(origin, title, artist, id);

CREATE INDEX IF NOT EXISTS idx_tracks_origin_artist_album
    ON tracks(origin, artist, album);

CREATE INDEX IF NOT EXISTS idx_tracks_origin_album_artist
    ON tracks(origin, album, artist);
