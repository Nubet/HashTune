package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.track.Track;
import com.norbertfila.hashtune.domain.track.TrackStatus;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository {
    Track save(Track track);

    Optional<Track> findById(UUID id);

    Optional<Track> findByChecksum(String checksum);

    Optional<Track> findFirstByStatus(TrackStatus status);

    void delete(Track track);
}
