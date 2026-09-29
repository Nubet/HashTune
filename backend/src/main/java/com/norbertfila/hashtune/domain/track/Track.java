package com.norbertfila.hashtune.domain.track;

import java.time.Instant;
import java.util.UUID;

public record Track(
        UUID id,
        String title,
        String artist,
        String album,
        String coverArtUrl,
        Long durationMs,
        String audioObjectKey,
        String checksum,
        TrackStatus status,
        Instant createdAt,
        Instant updatedAt) {}
