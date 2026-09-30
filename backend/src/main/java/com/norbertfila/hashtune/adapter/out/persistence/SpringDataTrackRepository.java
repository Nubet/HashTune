package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.domain.track.TrackOrigin;
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

    @Query("""
            SELECT track FROM TrackEntity track
            WHERE (:query = '' OR LOWER(track.title) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(track.artist) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(track.album) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:origin IS NULL OR track.origin = :origin)
              AND (:artist = '' OR track.artist = :artist)
              AND (:album = '' OR track.album = :album)
            """)
    Page<TrackEntity> searchTracks(
            @Param("query") String query,
            @Param("origin") TrackOrigin origin,
            @Param("artist") String artist,
            @Param("album") String album,
            Pageable pageable);

    @Query(value = """
            SELECT t.album AS title,
                   t.artist AS artist,
                   MAX(t.cover_art_url) AS cover_art_url,
                   (array_agg(t.id ORDER BY t.created_at)
                       FILTER (WHERE t.cover_art_object_key IS NOT NULL))[1] AS cover_art_track_id,
                   COUNT(t.id) AS track_count
            FROM tracks t
            WHERE t.album IS NOT NULL
              AND (:query = '' OR LOWER(t.album) LIKE CONCAT('%', LOWER(:query), '%')
                   OR LOWER(t.artist) LIKE CONCAT('%', LOWER(:query), '%'))
              AND (CAST(:origin AS VARCHAR) IS NULL OR t.origin = CAST(:origin AS VARCHAR))
            GROUP BY t.album, t.artist
            ORDER BY t.album, t.artist
            """, countQuery = """
            SELECT COUNT(*) FROM (
                SELECT t.album, t.artist
                FROM tracks t
                WHERE t.album IS NOT NULL
                  AND (:query = '' OR LOWER(t.album) LIKE CONCAT('%', LOWER(:query), '%')
                       OR LOWER(t.artist) LIKE CONCAT('%', LOWER(:query), '%'))
                  AND (CAST(:origin AS VARCHAR) IS NULL OR t.origin = CAST(:origin AS VARCHAR))
                GROUP BY t.album, t.artist
            ) albums
            """, nativeQuery = true)
    Page<AlbumSummaryProjection> searchAlbums(
            @Param("query") String query, @Param("origin") String origin, Pageable pageable);

    @Query("""
            SELECT track.artist AS name,
                   COUNT(track.id) AS trackCount,
                   COUNT(DISTINCT track.album) AS albumCount
            FROM TrackEntity track
            WHERE (:query = '' OR LOWER(track.artist) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:origin IS NULL OR track.origin = :origin)
            GROUP BY track.artist
            """)
    Page<ArtistSummaryProjection> searchArtists(
            @Param("query") String query, @Param("origin") TrackOrigin origin, Pageable pageable);
}
