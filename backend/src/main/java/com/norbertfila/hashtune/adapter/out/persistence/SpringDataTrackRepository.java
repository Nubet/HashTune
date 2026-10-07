package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.domain.track.TrackStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataTrackRepository extends JpaRepository<TrackEntity, UUID> {
    Optional<TrackEntity> findByChecksum(String checksum);

    Optional<TrackEntity> findFirstByStatusOrderByCreatedAtAsc(TrackStatus status);

    @Query(value = """
            SELECT track.*
            FROM tracks track
            WHERE (:query = '' OR LOWER(track.title) LIKE CONCAT('%', LOWER(:query), '%')
               OR LOWER(track.artist) LIKE CONCAT('%', LOWER(:query), '%')
               OR LOWER(track.album) LIKE CONCAT('%', LOWER(:query), '%'))
              AND (CAST(:origin AS VARCHAR) IS NULL OR track.origin = CAST(:origin AS VARCHAR))
              AND (
                    :artist = ''
                    OR EXISTS (
                        SELECT 1
                        FROM regexp_split_to_table(track.artist, '\\s*(?:&|,)\\s*') participant
                        WHERE LOWER(TRIM(participant)) = LOWER(:artist)
                    )
              )
              AND (:album = '' OR track.album = :album)
            ORDER BY track.title, track.artist, track.id
            """, countQuery = """
            SELECT COUNT(*)
            FROM tracks track
            WHERE (:query = '' OR LOWER(track.title) LIKE CONCAT('%', LOWER(:query), '%')
               OR LOWER(track.artist) LIKE CONCAT('%', LOWER(:query), '%')
               OR LOWER(track.album) LIKE CONCAT('%', LOWER(:query), '%'))
              AND (CAST(:origin AS VARCHAR) IS NULL OR track.origin = CAST(:origin AS VARCHAR))
              AND (
                    :artist = ''
                    OR EXISTS (
                        SELECT 1
                        FROM regexp_split_to_table(track.artist, '\\s*(?:&|,)\\s*') participant
                        WHERE LOWER(TRIM(participant)) = LOWER(:artist)
                    )
              )
              AND (:album = '' OR track.album = :album)
            """, nativeQuery = true)
    Page<TrackEntity> searchTracks(
            @Param("query") String query,
            @Param("origin") String origin,
            @Param("artist") String artist,
            @Param("album") String album,
            Pageable pageable);

    @Query(value = """
            SELECT t.album AS title,
                   COALESCE(MAX(NULLIF(TRIM(t.album_artist), '')), MIN(t.artist)) AS artist,
                   MAX(t.cover_art_url) AS cover_art_url,
                   (array_agg(t.id ORDER BY t.created_at)
                       FILTER (WHERE t.cover_art_object_key IS NOT NULL))[1] AS cover_art_track_id,
                   COUNT(t.id) AS track_count
            FROM tracks t
            WHERE t.album IS NOT NULL
               AND (:query = '' OR LOWER(t.album) LIKE CONCAT('%', LOWER(:query), '%')
                    OR LOWER(t.artist) LIKE CONCAT('%', LOWER(:query), '%')
                    OR LOWER(t.album_artist) LIKE CONCAT('%', LOWER(:query), '%'))
              AND (CAST(:origin AS VARCHAR) IS NULL OR t.origin = CAST(:origin AS VARCHAR))
             GROUP BY t.album, COALESCE(NULLIF(TRIM(t.album_artist), ''), '')
             ORDER BY t.album, artist
             """, countQuery = """
            SELECT COUNT(*) FROM (
                SELECT t.album, COALESCE(NULLIF(TRIM(t.album_artist), ''), '') AS album_artist
                FROM tracks t
                WHERE t.album IS NOT NULL
                   AND (:query = '' OR LOWER(t.album) LIKE CONCAT('%', LOWER(:query), '%')
                        OR LOWER(t.artist) LIKE CONCAT('%', LOWER(:query), '%')
                        OR LOWER(t.album_artist) LIKE CONCAT('%', LOWER(:query), '%'))
                  AND (CAST(:origin AS VARCHAR) IS NULL OR t.origin = CAST(:origin AS VARCHAR))
                GROUP BY t.album, COALESCE(NULLIF(TRIM(t.album_artist), ''), '')
            ) albums
            """, nativeQuery = true)
    Page<AlbumSummaryProjection> searchAlbums(
            @Param("query") String query, @Param("origin") String origin, Pageable pageable);

    @Query(value = """
            SELECT TRIM(participant) AS name,
                   COUNT(DISTINCT track.id) AS track_count,
                   COUNT(DISTINCT track.album) AS album_count
            FROM tracks track
            CROSS JOIN LATERAL regexp_split_to_table(track.artist, '\\s*(?:&|,)\\s*') participant
            WHERE NULLIF(TRIM(participant), '') IS NOT NULL
              AND (:query = '' OR LOWER(TRIM(participant)) LIKE CONCAT('%', LOWER(:query), '%'))
              AND (CAST(:origin AS VARCHAR) IS NULL OR track.origin = CAST(:origin AS VARCHAR))
            GROUP BY TRIM(participant)
            ORDER BY name
            """, countQuery = """
            SELECT COUNT(*)
            FROM (
                SELECT TRIM(participant)
                FROM tracks track
                CROSS JOIN LATERAL regexp_split_to_table(track.artist, '\\s*(?:&|,)\\s*') participant
                WHERE NULLIF(TRIM(participant), '') IS NOT NULL
                  AND (:query = '' OR LOWER(TRIM(participant)) LIKE CONCAT('%', LOWER(:query), '%'))
                  AND (CAST(:origin AS VARCHAR) IS NULL OR track.origin = CAST(:origin AS VARCHAR))
                GROUP BY TRIM(participant)
            ) artists
            """, nativeQuery = true)
    Page<ArtistSummaryProjection> searchArtists(
            @Param("query") String query, @Param("origin") String origin, Pageable pageable);
}
