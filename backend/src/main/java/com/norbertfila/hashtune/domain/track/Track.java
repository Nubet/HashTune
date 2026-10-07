package com.norbertfila.hashtune.domain.track;

import java.time.Instant;
import java.util.UUID;

public record Track(
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
        String coverArtObjectKey,
        String coverArtMimeType,
        Long durationMs,
        String audioObjectKey,
        String checksum,
        TrackStatus status,
        Instant createdAt,
        Instant updatedAt) {
    public Track(
            UUID id,
            String title,
            String artist,
            String album,
            TrackOrigin origin,
            String coverArtUrl,
            Long durationMs,
            String audioObjectKey,
            String checksum,
            TrackStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this(
                id,
                title,
                artist,
                album,
                origin,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                coverArtUrl,
                null,
                null,
                durationMs,
                audioObjectKey,
                checksum,
                status,
                createdAt,
                updatedAt);
    }

    public Track(
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
            Instant updatedAt) {
        this(
                id,
                title,
                artist,
                album,
                TrackOrigin.HASH_TUNE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                coverArtUrl,
                null,
                null,
                durationMs,
                audioObjectKey,
                checksum,
                status,
                createdAt,
                updatedAt);
    }

    public Track withStatus(TrackStatus nextStatus) {
        return withDurationAndStatus(durationMs, nextStatus);
    }

    public Track withDurationAndStatus(Long nextDurationMs, TrackStatus nextStatus) {
        return new Track(
                id,
                title,
                artist,
                album,
                origin,
                albumArtist,
                composer,
                genre,
                releaseYear,
                trackNumber,
                discNumber,
                isrc,
                barcode,
                comment,
                coverArtUrl,
                coverArtObjectKey,
                coverArtMimeType,
                nextDurationMs,
                audioObjectKey,
                checksum,
                nextStatus,
                createdAt,
                Instant.now());
    }

    public Track withoutAudioObjectKey() {
        return new Track(
                id,
                title,
                artist,
                album,
                origin,
                albumArtist,
                composer,
                genre,
                releaseYear,
                trackNumber,
                discNumber,
                isrc,
                barcode,
                comment,
                coverArtUrl,
                coverArtObjectKey,
                coverArtMimeType,
                durationMs,
                null,
                checksum,
                status,
                createdAt,
                Instant.now());
    }
}
