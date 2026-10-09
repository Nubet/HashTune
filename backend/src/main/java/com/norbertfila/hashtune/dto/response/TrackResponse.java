package com.norbertfila.hashtune.dto.response;

import com.norbertfila.hashtune.entity.track.TrackOrigin;
import java.time.Instant;
import java.util.UUID;

public record TrackResponse(
        UUID id,
        String title,
        String artist,
        String album,
        TrackOrigin origin,
        String albumArtist,
        String composer,
        String genre,
        String releaseYear,
        Integer trackNumber,
        Integer discNumber,
        String isrc,
        String barcode,
        String comment,
        String coverArtUrl,
        Long durationMs,
        String status,
        Instant createdAt) {}
