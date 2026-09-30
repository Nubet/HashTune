package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.domain.track.TrackStatus;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
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
            WHERE (:origin IS NULL OR track.origin = :origin)
               AND (LOWER(track.title) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(track.artist) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(track.album) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY track.createdAt DESC
            """)
    Page<TrackEntity> searchByQuery(
            @Param("query") String query, @Param("origin") TrackOrigin origin, Pageable pageable);

    Page<TrackEntity> findByOrigin(TrackOrigin origin, Pageable pageable);
}
