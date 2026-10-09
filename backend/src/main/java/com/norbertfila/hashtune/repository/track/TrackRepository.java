package com.norbertfila.hashtune.repository.track;

import com.norbertfila.hashtune.entity.track.Track;
import com.norbertfila.hashtune.entity.track.TrackStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository {
    Track save(Track track);

    Optional<Track> findById(UUID id);

    Optional<Track> findByChecksum(String checksum);

    List<Track> findAll();

    Optional<Track> findFirstByStatus(TrackStatus status);

    void delete(Track track);
}
