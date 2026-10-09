package com.norbertfila.hashtune.service.track;

import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.entity.indexing.IndexingJob;
import com.norbertfila.hashtune.entity.indexing.IndexingJobStatus;
import com.norbertfila.hashtune.entity.track.Track;
import com.norbertfila.hashtune.entity.track.TrackOrigin;
import com.norbertfila.hashtune.entity.track.TrackStatus;
import com.norbertfila.hashtune.exceptions.application.ApplicationException;
import com.norbertfila.hashtune.exceptions.storage.StorageException;
import com.norbertfila.hashtune.repository.fingerprint.FingerprintRepository;
import com.norbertfila.hashtune.repository.indexing.IndexingJobRepository;
import com.norbertfila.hashtune.repository.track.TrackRepository;
import com.norbertfila.hashtune.service.metadata.AudioMetadataReader;
import com.norbertfila.hashtune.service.metadata.CoverArtProvider;
import com.norbertfila.hashtune.service.storage.ObjectStoragePort;
import com.norbertfila.hashtune.service.validation.AudioUploadValidator;
import com.norbertfila.hashtune.service.validation.BoundedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackService {
    private final TrackRepository tracks;
    private final IndexingJobRepository jobs;
    private final ObjectStoragePort storage;
    private final FingerprintRepository fingerprints;
    private final StorageProperties storageProperties;
    private final AudioSafetyProperties audioSafetyProperties;
    private final AudioUploadValidator audioUploadValidator;
    private final AudioMetadataReader metadataReader;
    private final CoverArtProvider coverArtProvider;

    @Transactional
    public UploadResult upload(MultipartFile file) {
        validate(file);
        String checksum = checksum(file);
        ensureNew(checksum);
        return saveUpload(file, checksum, TrackOrigin.HASH_TUNE, null, IndexingJobStatus.AWAITING_CONFIRMATION);
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
        List<String> uploadedKeys = new ArrayList<>();
        try (InputStream input = file.getInputStream()) {
            uploadedKeys.add(key);
            storage.put(storageProperties.getAudioBucket(), key, input, file.getSize(), file.getContentType());
            if (metadata.artwork() != null) {
                uploadedKeys.add(coverArtObjectKey);
                storage.put(
                        storageProperties.getAudioBucket(),
                        coverArtObjectKey,
                        new ByteArrayInputStream(metadata.artwork().data()),
                        metadata.artwork().data().length,
                        coverArtMimeType);
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
            IndexingJob job = jobs.save(new IndexingJob(
                    UUID.randomUUID(), track.id(), initialJobStatus, 0, 0, null, null, now, null, null));
            return new UploadResult(track, job);
        } catch (IOException exception) {
            cleanupUploadedObjects(uploadedKeys);
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AUDIO", "Could not read uploaded file");
        } catch (RuntimeException exception) {
            cleanupUploadedObjects(uploadedKeys);
            throw exception;
        }
    }

    @Transactional
    public void delete(UUID id) {
        Track track = get(id);
        fingerprints.deleteByTrackId(track.id());
        if (track.audioObjectKey() != null) {
            storage.delete(storageProperties.getAudioBucket(), track.audioObjectKey());
        }
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
        Track updatedTrack = tracks.save(new Track(
                track.id(),
                updatedTitle,
                updatedArtist,
                updatedAlbum,
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
                track.durationMs(),
                track.audioObjectKey(),
                track.checksum(),
                track.status(),
                track.createdAt(),
                Instant.now()));
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
        Track track = tracks.save(get(id).withStatus(TrackStatus.UPLOADED));
        IndexingJob existingJob = jobs.findByTrackId(track.id()).orElse(null);
        if (existingJob != null && existingJob.status() == IndexingJobStatus.PROCESSING) {
            return existingJob;
        }
        return jobs.save(resetJob(existingJob, track.id()));
    }

    @Transactional
    public ReindexAllResult reindexAll() {
        int scheduled = 0;
        int alreadyProcessing = 0;
        int awaitingConfirmation = 0;
        for (Track track : tracks.findAll()) {
            IndexingJob job = jobs.findByTrackId(track.id()).orElse(null);
            if (job != null && job.status() == IndexingJobStatus.AWAITING_CONFIRMATION) {
                awaitingConfirmation++;
                continue;
            }
            if (job != null && job.status() == IndexingJobStatus.PROCESSING) {
                alreadyProcessing++;
                continue;
            }
            tracks.save(track.withStatus(TrackStatus.UPLOADED));
            jobs.save(resetJob(job, track.id()));
            scheduled++;
        }
        return new ReindexAllResult(scheduled, alreadyProcessing, awaitingConfirmation);
    }

    private IndexingJob resetJob(IndexingJob job, UUID trackId) {
        Instant createdAt = job == null ? Instant.now() : job.createdAt();
        return new IndexingJob(
                job == null ? UUID.randomUUID() : job.id(),
                trackId,
                IndexingJobStatus.PENDING,
                0,
                0,
                null,
                null,
                createdAt,
                null,
                null,
                null);
    }

    private void cleanupUploadedObjects(List<String> objectKeys) {
        for (String objectKey : objectKeys) {
            try {
                storage.delete(storageProperties.getAudioBucket(), objectKey);
            } catch (RuntimeException cleanupFailure) {
                log.warn("Could not clean up uploaded object {}", objectKey, cleanupFailure);
            }
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
        if (!audioSafetyProperties.getAllowedCoverArtContentTypes().contains(track.coverArtMimeType())) {
            throw notFound("COVER_ART_NOT_FOUND", "Embedded cover art not found");
        }
        try {
            InputStream input = storage.get(storageProperties.getAudioBucket(), track.coverArtObjectKey());
            return new CoverArt(
                    new BoundedInputStream(input, audioSafetyProperties.getMaxCoverArtBytes()),
                    track.coverArtMimeType());
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
        audioUploadValidator.validate(file);
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

    public record ReindexAllResult(int scheduled, int alreadyProcessing, int awaitingConfirmation) {}

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

    public record CoverArt(InputStream content, String mimeType) {}
}
