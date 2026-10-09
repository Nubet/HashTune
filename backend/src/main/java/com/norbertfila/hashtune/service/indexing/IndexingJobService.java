package com.norbertfila.hashtune.service.indexing;

import com.norbertfila.hashtune.service.fingerprint.AudioRecognitionEngine;
import com.norbertfila.hashtune.exceptions.audio.AudioInputRejectedException;
import com.norbertfila.hashtune.exceptions.ApplicationException;
import com.norbertfila.hashtune.repository.indexing.IndexingJobRepository;
import com.norbertfila.hashtune.service.storage.ObjectStoragePort;
import com.norbertfila.hashtune.repository.track.TrackRepository;
import com.norbertfila.hashtune.configuration.IndexingProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.entity.indexing.IndexingJob;
import com.norbertfila.hashtune.entity.indexing.IndexingJobStatus;
import com.norbertfila.hashtune.entity.track.Track;
import com.norbertfila.hashtune.entity.track.TrackStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingJobService {
    private final TrackRepository tracks;
    private final IndexingJobRepository jobs;
    private final AudioRecognitionEngine engine;
    private final StorageProperties storageProperties;
    private final ObjectStoragePort storage;
    private final IndexingProperties indexingProperties;
    private final Executor indexingExecutor;

    @Transactional
    public IndexingJob retry(UUID id) {
        IndexingJob job = get(id);
        if (job.status() != IndexingJobStatus.FAILED) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "INDEXING_JOB_NOT_RETRYABLE",
                    "Only failed indexing jobs can be retried");
        }
        Track track = getTrack(job.trackId());
        tracks.save(track.withStatus(TrackStatus.UPLOADED));
        return jobs.save(new IndexingJob(
                job.id(),
                job.trackId(),
                IndexingJobStatus.PENDING,
                0,
                0,
                null,
                null,
                job.createdAt(),
                null,
                null,
                Instant.now()));
    }

    public IndexingJob get(UUID id) {
        return jobs.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "INDEXING_JOB_NOT_FOUND",
                        "Indexing job not found"));
    }

    @Scheduled(fixedDelayString = "${app.indexing.worker-delay-ms:1000}")
    public void processNextJob() {
        if (!indexingProperties.isWorkerEnabled()) return;
        Instant now = Instant.now();
        jobs.recoverStaleProcessing(now.minusMillis(indexingProperties.getStaleProcessingTimeoutMs()), now);
        jobs.claimNextPending(now).ifPresent(job -> indexingExecutor.execute(() -> process(job)));
    }

    private void process(IndexingJob job) {
        Track track = null;
        try {
            track = getTrack(job.trackId());
            AudioRecognitionEngine.IndexingResult result = engine.index(
                    track.id(),
                    new AudioRecognitionEngine.InputAudio(
                            storageProperties.getAudioBucket(),
                            track.audioObjectKey(),
                            track.title(),
                            track.checksum()));
            Track indexedTrack = track.withDurationAndStatus(result.durationMs(), TrackStatus.INDEXED);
            if (!storageProperties.isRetainLibraryAudio()) {
                indexedTrack = indexedTrack.withoutAudioObjectKey();
            }
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
                    Instant.now(),
                    null));
            tracks.save(indexedTrack);
            if (!storageProperties.isRetainLibraryAudio() && track.audioObjectKey() != null) {
                try {
                    storage.delete(storageProperties.getAudioBucket(), track.audioObjectKey());
                } catch (RuntimeException exception) {
                    log.warn("Could not delete retained library audio for track {}", track.id(), exception);
                }
            }
        } catch (Exception exception) {
            handleFailure(job, track, exception);
        }
    }

    private void handleFailure(IndexingJob job, Track track, Exception exception) {
        boolean retry = isRetryable(exception)
                && job.attempts() < Math.max(1, indexingProperties.getMaxAttempts());
        Instant now = Instant.now();
        jobs.save(new IndexingJob(
                job.id(),
                job.trackId(),
                retry ? IndexingJobStatus.PENDING : IndexingJobStatus.FAILED,
                retry ? 0 : job.progress(),
                job.attempts(),
                "INDEXING_FAILED",
                errorMessage(exception),
                job.createdAt(),
                retry ? null : job.startedAt(),
                retry ? null : now,
                retry ? now.plus(retryDelay(job.attempts())) : null));
        if (track != null) {
            tracks.save(track.withStatus(retry ? TrackStatus.UPLOADED : TrackStatus.FAILED));
        }
    }

    private Duration retryDelay(int attempts) {
        long multiplier = 1L << Math.min(Math.max(attempts - 1, 0), 30);
        long maxDelay = Math.max(0, indexingProperties.getMaxRetryBackoffMs());
        long baseDelay = Math.max(0, indexingProperties.getRetryBackoffMs());
        long delay = baseDelay > maxDelay / multiplier ? maxDelay : Math.min(maxDelay, baseDelay * multiplier);
        return Duration.ofMillis(delay);
    }

    private boolean isRetryable(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof AudioInputRejectedException) {
                return false;
            }
            current = current.getCause();
        }
        return true;
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }

    private Track getTrack(UUID id) {
        return tracks.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "TRACK_NOT_FOUND", "Track not found"));
    }
}
