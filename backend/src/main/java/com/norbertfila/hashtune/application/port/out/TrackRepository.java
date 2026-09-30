package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.track.Track;
import com.norbertfila.hashtune.domain.track.TrackStatus;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository {
    Track save(Track track);

    Optional<Track> findById(UUID id);

    Optional<Track> findByChecksum(String checksum);

    Optional<Track> findFirstByStatus(TrackStatus status);

    List<Track> search(String query, TrackOrigin origin, int limit, int offset);

    void delete(Track track);
}
