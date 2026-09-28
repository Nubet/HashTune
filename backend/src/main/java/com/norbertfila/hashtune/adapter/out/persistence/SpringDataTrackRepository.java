package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.domain.track.TrackStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTrackRepository extends JpaRepository<TrackEntity, UUID> {
    Optional<TrackEntity> findByChecksum(String checksum);

    Optional<TrackEntity> findFirstByStatusOrderByCreatedAtAsc(TrackStatus status);
}
