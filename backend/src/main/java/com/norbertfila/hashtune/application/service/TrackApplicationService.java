package com.norbertfila.hashtune.application.service;

import com.norbertfila.hashtune.application.port.out.AudioRecognitionEngine;
import com.norbertfila.hashtune.application.port.out.IndexingJobRepository;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.application.port.out.TrackRepository;
import com.norbertfila.hashtune.configuration.AudioProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.domain.indexing.IndexingJobStatus;
import com.norbertfila.hashtune.domain.indexing.IndexingJob;
import com.norbertfila.hashtune.domain.track.Track;
import com.norbertfila.hashtune.domain.track.TrackStatus;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
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
    private final AudioRecognitionEngine engine;
    private final StorageProperties storageProperties;
    private final AudioProperties audioProperties;

    @Transactional
    public UploadResult upload(MultipartFile file) {
        validate(file);
        String checksum = checksum(file);
        tracks.findByChecksum(checksum).ifPresent(existing -> {
            throw new ApplicationException(org.springframework.http.HttpStatus.CONFLICT, "TRACK_ALREADY_EXISTS", "Track already exists");
        });
        UUID id = UUID.randomUUID();
        String key = "audio/" + id + "/original-" + safeName(file.getOriginalFilename());
        try (InputStream input = file.getInputStream()) {
            storage.put(storageProperties.getAudioBucket(), key, input, file.getSize(), file.getContentType());
        } catch (IOException exception) {
            throw new ApplicationException(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AUDIO", "Could not read uploaded file");
        }
        Instant now = Instant.now();
        Track track = tracks.save(new Track(id, title(file), "Unknown", null, null, key, checksum, TrackStatus.UPLOADED, now, now));
        IndexingJob job = jobs.save(new IndexingJob(UUID.randomUUID(), track.id(), IndexingJobStatus.PENDING, 0, 0, null, null, now, null, null));
        return new UploadResult(track, job);
    }

    public List<Track> search(String query, int limit, int offset) {
        return tracks.search(query, Math.min(limit, 100), Math.max(offset, 0));
    }

    @Transactional
    public void delete(UUID id) {
        Track track = get(id);
        storage.delete(storageProperties.getAudioBucket(), track.audioObjectKey());
        tracks.delete(track);
    }

    @Transactional
    public IndexingJob reindex(UUID id) {
        Track track = get(id);
        track = tracks.save(new Track(track.id(), track.title(), track.artist(), track.album(), track.durationMs(), track.audioObjectKey(), track.checksum(), TrackStatus.UPLOADED, track.createdAt(), Instant.now()));
        return jobs.save(new IndexingJob(UUID.randomUUID(), track.id(), IndexingJobStatus.PENDING, 0, 0, null, null, Instant.now(), null, null));
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
        try (InputStream ignored = storage.get(storageProperties.getAudioBucket(), track.audioObjectKey())) {
            AudioRecognitionEngine.IndexingResult result = engine.index(track.id(), new AudioRecognitionEngine.InputAudio(
                    storageProperties.getAudioBucket(), track.audioObjectKey(), track.title(), track.checksum()));
            jobs.save(new IndexingJob(job.id(), job.trackId(), IndexingJobStatus.COMPLETED, 100, job.attempts(), null, null,
                    job.createdAt(), job.startedAt(), Instant.now()));
            tracks.save(new Track(track.id(), track.title(), track.artist(), track.album(), result.durationMs(), track.audioObjectKey(),
                    track.checksum(), TrackStatus.INDEXED, track.createdAt(), Instant.now()));
        } catch (Exception exception) {
            jobs.save(new IndexingJob(job.id(), job.trackId(), IndexingJobStatus.FAILED, job.progress(), job.attempts(),
                    "INDEXING_FAILED", exception.getMessage(), job.createdAt(), job.startedAt(), Instant.now()));
            tracks.save(new Track(track.id(), track.title(), track.artist(), track.album(), track.durationMs(), track.audioObjectKey(),
                    track.checksum(), TrackStatus.FAILED, track.createdAt(), Instant.now()));
        }
    }

    private Track get(UUID id) {
        return tracks.findById(id).orElseThrow(() -> notFound("TRACK_NOT_FOUND", "Track not found"));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > audioProperties.getMaxFileSizeBytes()) {
            throw new ApplicationException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_AUDIO", "Audio file is empty or too large");
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

    private String title(MultipartFile file) {
        String name = safeName(file.getOriginalFilename());
        int extension = name.lastIndexOf('.');
        return extension > 0 ? name.substring(0, extension) : name;
    }

    private String safeName(String name) {
        return name == null ? "audio" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private ApplicationException notFound(String code, String message) {
        return new ApplicationException(org.springframework.http.HttpStatus.NOT_FOUND, code, message);
    }

    public record UploadResult(Track track, IndexingJob job) { }
}
