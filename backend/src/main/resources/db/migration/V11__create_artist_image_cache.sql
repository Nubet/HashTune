CREATE TABLE IF NOT EXISTS artist_image_cache (
    artist_key VARCHAR(255) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    image_url VARCHAR(2048),
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL
);
