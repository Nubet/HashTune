package com.norbertfila.hashtune.application.service;

import com.norbertfila.hashtune.adapter.out.metadata.AudioMetadataReader;
import com.norbertfila.hashtune.adapter.out.storage.StorageException;
import com.norbertfila.hashtune.application.port.out.AudioRecognitionEngine;
import com.norbertfila.hashtune.application.port.out.CoverArtProvider;
import com.norbertfila.hashtune.application.port.out.FingerprintRepository;
import com.norbertfila.hashtune.application.port.out.IndexingJobRepository;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.application.port.out.TrackRepository;
import com.norbertfila.hashtune.configuration.AudioProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.domain.indexing.IndexingJob;
import com.norbertfila.hashtune.domain.indexing.IndexingJobStatus;
import com.norbertfila.hashtune.domain.track.Track;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
import com.norbertfila.hashtune.domain.track.TrackStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TrackApplicationService {
    private final TrackRepository tracks;
    private final IndexingJobRepository jobs;
    private final ObjectStoragePort storage;
    private final FingerprintRepository fingerprints;
    private final AudioRecognitionEngine engine;
    private final StorageProperties storageProperties;
    private final AudioProperties audioProperties;
    private final AudioMetadataReader metadataReader;
    private final CoverArtProvider coverArtProvider;

    @Transactional
    public UploadResult upload(MultipartFile file) {
        validate(file);
        String checksum = checksum(file);
        ensureNew(checksum);
        return saveUpload(file, checksum, TrackOrigin.PERSONAL, null, IndexingJobStatus.AWAITING_CONFIRMATION);
    }

    @Transactional
    public ImportResult importTrack(MultipartFile file, TrackOrigin origin, String relativePath) {
        validate(file);
        String checksum = checksum(file);
        Track existing = tracks.findByChecksum(checksum).orElse(null);
        if (existing != null) {
            return new ImportResult(
                    "DUPLICATE", existing, jobs.findByTrackId(existing.id()).orElse(null));
        }
        UploadResult result = saveUpload(file, checksum, origin, relativePath, IndexingJobStatus.PENDING);
        return new ImportResult("IMPORTED", result.track(), result.job());
    }

    private UploadResult saveUpload(
            MultipartFile file,
            String checksum,
            TrackOrigin origin,
            String relativePath,
            IndexingJobStatus initialJobStatus) {
        UUID id = UUID.randomUUID();
        String key = "audio/" + id + "/original-" + safeName(file.getOriginalFilename());
        AudioMetadataReader.AudioMetadata metadata = metadataReader.read(file);
        PathMetadata pathMetadata = PathMetadata.from(relativePath, file.getOriginalFilename());
        String title = firstValue(metadata.title(), pathMetadata.title());
        String artist = firstValue(metadata.artist(), firstValue(pathMetadata.artist(), "Unknown"));
        String album = firstValue(metadata.album(), pathMetadata.album());
        String coverArtObjectKey = metadata.artwork() == null ? null : "artwork/" + id + "/cover";
        String coverArtMimeType =
                metadata.artwork() == null ? null : metadata.artwork().mimeType();
        String coverArtUrl = metadata.artwork() == null
                ? coverArtProvider.findCoverArt(title, artist, album).orElse(null)
                : null;
        try (InputStream input = file.getInputStream()) {
            storage.put(storageProperties.getAudioBucket(), key, input, file.getSize(), file.getContentType());
            if (metadata.artwork() != null) {
                storage.put(
                        storageProperties.getAudioBucket(),
                        coverArtObjectKey,
                        new ByteArrayInputStream(metadata.artwork().data()),
                        metadata.artwork().data().length,
                        coverArtMimeType);
            }
        } catch (IOException exception) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AUDIO", "Could not read uploaded file");
        }
        Instant now = Instant.now();
        Track track = tracks.save(new Track(
                id,
                title,
                artist,
                album,
                origin,
                metadata.albumArtist(),
                metadata.composer(),
                metadata.genre(),
                metadata.releaseYear(),
                metadata.trackNumber(),
                metadata.discNumber(),
                metadata.isrc(),
                metadata.barcode(),
                metadata.comment(),
                coverArtUrl,
                coverArtObjectKey,
                coverArtMimeType,
                null,
                key,
                checksum,
                TrackStatus.UPLOADED,
                now,
                now));
        IndexingJob job = jobs.save(
                new IndexingJob(UUID.randomUUID(), track.id(), initialJobStatus, 0, 0, null, null, now, null, null));
        return new UploadResult(track, job);
    }

    @Transactional
    public void delete(UUID id) {
        Track track = get(id);
        fingerprints.deleteByTrackId(track.id());
        storage.delete(storageProperties.getAudioBucket(), track.audioObjectKey());
        if (track.coverArtObjectKey() != null) {
            storage.delete(storageProperties.getAudioBucket(), track.coverArtObjectKey());
        }
        tracks.delete(track);
    }

    @Transactional
    public Track updateMetadata(UUID id, String title, String artist, String album) {
        Track track = get(id);
        String updatedTitle = firstValue(title, track.title());
        String updatedArtist = firstValue(artist, "Unknown");
        String updatedAlbum = album == null || album.isBlank() ? null : album.trim();
        String coverArtUrl = coverArtProvider
                .findCoverArt(updatedTitle, updatedArtist, updatedAlbum)
                .orElse(null);
        Track updatedTrack = tracks.save(copyWith(
                track, updatedTitle, updatedArtist, updatedAlbum, coverArtUrl, track.durationMs(), track.status()));
        jobs.findByTrackId(id)
                .filter(job -> job.status() == IndexingJobStatus.AWAITING_CONFIRMATION)
                .ifPresent(job -> jobs.save(new IndexingJob(
                        job.id(),
                        job.trackId(),
                        IndexingJobStatus.PENDING,
                        job.progress(),
                        job.attempts(),
                        job.errorCode(),
                        job.errorMessage(),
                        job.createdAt(),
                        job.startedAt(),
                        job.finishedAt())));
        return updatedTrack;
    }

    @Transactional
    public IndexingJob reindex(UUID id) {
        Track track = get(id);
        track = tracks.save(copyWith(
                track,
                track.title(),
                track.artist(),
                track.album(),
                track.coverArtUrl(),
                track.durationMs(),
                TrackStatus.UPLOADED));
        return jobs.save(new IndexingJob(
                UUID.randomUUID(), track.id(), IndexingJobStatus.PENDING, 0, 0, null, null, Instant.now(), null, null));
    }

    public IndexingJob getJob(UUID id) {
        return jobs.findById(id).orElseThrow(() -> notFound("INDEXING_JOB_NOT_FOUND", "Indexing job not found"));
    }

    @Scheduled(fixedDelayString = "${app.indexing.worker-delay-ms:1000}")
    public void processNextJob() {
        jobs.claimNextPending().ifPresent(this::process);
    }

    private void process(IndexingJob job) {
        Track track = get(job.trackId());
        try {
            AudioRecognitionEngine.IndexingResult result = engine.index(
                    track.id(),
                    new AudioRecognitionEngine.InputAudio(
                            storageProperties.getAudioBucket(),
                            track.audioObjectKey(),
                            track.title(),
                            track.checksum()));
            jobs.save(new IndexingJob(
                    job.id(),
                    job.trackId(),
                    IndexingJobStatus.COMPLETED,
                    100,
                    job.attempts(),
                    null,
                    null,
                    job.createdAt(),
                    job.startedAt(),
                    Instant.now()));
            tracks.save(copyWith(
                    track,
                    track.title(),
                    track.artist(),
                    track.album(),
                    track.coverArtUrl(),
                    result.durationMs(),
                    TrackStatus.INDEXED));
        } catch (Exception exception) {
            jobs.save(new IndexingJob(
                    job.id(),
                    job.trackId(),
                    IndexingJobStatus.FAILED,
                    job.progress(),
                    job.attempts(),
                    "INDEXING_FAILED",
                    exception.getMessage(),
                    job.createdAt(),
                    job.startedAt(),
                    Instant.now()));
            tracks.save(copyWith(
                    track,
                    track.title(),
                    track.artist(),
                    track.album(),
                    track.coverArtUrl(),
                    track.durationMs(),
                    TrackStatus.FAILED));
        }
    }

    private Track get(UUID id) {
        return tracks.findById(id).orElseThrow(() -> notFound("TRACK_NOT_FOUND", "Track not found"));
    }

    public CoverArt getCoverArt(UUID id) {
        Track track = get(id);
        if (track.coverArtObjectKey() == null) {
            throw notFound("COVER_ART_NOT_FOUND", "Embedded cover art not found");
        }
        try (InputStream input = storage.get(storageProperties.getAudioBucket(), track.coverArtObjectKey())) {
            return new CoverArt(input.readAllBytes(), track.coverArtMimeType());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read cover art", exception);
        } catch (StorageException exception) {
            throw notFound("COVER_ART_NOT_FOUND", "Embedded cover art not found");
        }
    }

    private Track copyWith(
            Track track,
            String title,
            String artist,
            String album,
            String coverArtUrl,
            Long durationMs,
            TrackStatus status) {
        return new Track(
                track.id(),
                title,
                artist,
                album,
                track.origin(),
                track.albumArtist(),
                track.composer(),
                track.genre(),
                track.releaseYear(),
                track.trackNumber(),
                track.discNumber(),
                track.isrc(),
                track.barcode(),
                track.comment(),
                coverArtUrl,
                track.coverArtObjectKey(),
                track.coverArtMimeType(),
                durationMs,
                track.audioObjectKey(),
                track.checksum(),
                status,
                track.createdAt(),
                Instant.now());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > audioProperties.getMaxFileSizeBytes()) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_AUDIO",
                    "Audio file is empty or too large");
        }
    }

    private String checksum(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                digestInput.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not calculate checksum", exception);
        }
    }

    private String safeName(String name) {
        return name == null ? "audio" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String firstValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void ensureNew(String checksum) {
        tracks.findByChecksum(checksum).ifPresent(existing -> {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.CONFLICT, "TRACK_ALREADY_EXISTS", "Track already exists");
        });
    }

    private ApplicationException notFound(String code, String message) {
        return new ApplicationException(org.springframework.http.HttpStatus.NOT_FOUND, code, message);
    }

    public record UploadResult(Track track, IndexingJob job) {}

    public record ImportResult(String status, Track track, IndexingJob job) {}

    private record PathMetadata(String title, String artist, String album) {
        static PathMetadata from(String relativePath, String originalFilename) {
            String fallbackTitle = titleFrom(originalFilename);
            if (relativePath == null || relativePath.isBlank()) {
                return new PathMetadata(fallbackTitle, null, null);
            }
            String[] parts = relativePath.replace('\\', '/').split("/");
            if (parts.length < 3) {
                return new PathMetadata(fallbackTitle, null, null);
            }
            return new PathMetadata(fallbackTitle, parts[parts.length - 3], parts[parts.length - 2]);
        }

        private static String titleFrom(String filename) {
            String safe = filename == null || filename.isBlank() ? "audio" : filename;
            int extension = safe.lastIndexOf('.');
            return extension > 0 ? safe.substring(0, extension) : safe;
        }
    }

    public record CoverArt(byte[] data, String mimeType) {}
}
