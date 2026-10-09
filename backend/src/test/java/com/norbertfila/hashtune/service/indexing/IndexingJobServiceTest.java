package com.norbertfila.hashtune.service.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.norbertfila.hashtune.application.port.out.AudioRecognitionEngine;
import com.norbertfila.hashtune.application.port.out.AudioInputRejectedException;
import com.norbertfila.hashtune.repository.indexing.IndexingJobRepository;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.repository.track.TrackRepository;
import com.norbertfila.hashtune.configuration.IndexingProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.entity.indexing.IndexingJob;
import com.norbertfila.hashtune.entity.indexing.IndexingJobStatus;
import com.norbertfila.hashtune.entity.track.Track;
import com.norbertfila.hashtune.entity.track.TrackOrigin;
import com.norbertfila.hashtune.entity.track.TrackStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexingJobServiceTest {
    private final TrackRepository tracks = mock(TrackRepository.class);
    private final IndexingJobRepository jobs = mock(IndexingJobRepository.class);
    private final AudioRecognitionEngine engine = mock(AudioRecognitionEngine.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final StorageProperties storageProperties = new StorageProperties();
    private final IndexingProperties indexingProperties = new IndexingProperties();
    private IndexingJobService service;

    @BeforeEach
    void setUp() {
        storageProperties.setAudioBucket("audio");
        indexingProperties.setRetryBackoffMs(100);
        indexingProperties.setMaxRetryBackoffMs(1_000);
        indexingProperties.setStaleProcessingTimeoutMs(60_000);
        service = new IndexingJobService(
                tracks, jobs, engine, storageProperties, storage, indexingProperties, Runnable::run);
        when(jobs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void schedulesFailedIndexingJobForRetry() {
        Track track = track(TrackStatus.UPLOADED);
        IndexingJob job = job(track.id(), IndexingJobStatus.PENDING, 1);
        when(jobs.claimNextPending(any())).thenReturn(Optional.of(job));
        when(tracks.findById(track.id())).thenReturn(Optional.of(track));
        when(engine.index(eq(track.id()), any())).thenThrow(new IllegalStateException("ffmpeg failed"));

        service.processNextJob();

        var savedJob = org.mockito.ArgumentCaptor.forClass(IndexingJob.class);
        verify(jobs).save(savedJob.capture());
        assertThat(savedJob.getValue().status()).isEqualTo(IndexingJobStatus.PENDING);
        assertThat(savedJob.getValue().nextAttemptAt()).isNotNull();
        var savedTrack = org.mockito.ArgumentCaptor.forClass(Track.class);
        verify(tracks).save(savedTrack.capture());
        assertThat(savedTrack.getValue().status()).isEqualTo(TrackStatus.UPLOADED);
    }

    @Test
    void manuallyRetriesFailedJobWithFreshAttemptBudget() {
        Track track = track(TrackStatus.FAILED);
        IndexingJob job = job(track.id(), IndexingJobStatus.FAILED, 3);
        when(jobs.findById(job.id())).thenReturn(Optional.of(job));
        when(tracks.findById(track.id())).thenReturn(Optional.of(track));

        IndexingJob retried = service.retry(job.id());

        assertThat(retried.status()).isEqualTo(IndexingJobStatus.PENDING);
        assertThat(retried.attempts()).isZero();
        assertThat(retried.nextAttemptAt()).isNotNull();
    }

    @Test
    void doesNotRetryRejectedAudio() {
        Track track = track(TrackStatus.UPLOADED);
        IndexingJob job = job(track.id(), IndexingJobStatus.PENDING, 1);
        when(jobs.claimNextPending(any())).thenReturn(Optional.of(job));
        when(tracks.findById(track.id())).thenReturn(Optional.of(track));
        when(engine.index(eq(track.id()), any()))
                .thenThrow(new AudioInputRejectedException("AUDIO_DURATION_TOO_LONG", "too long"));

        service.processNextJob();

        var savedJob = org.mockito.ArgumentCaptor.forClass(IndexingJob.class);
        verify(jobs).save(savedJob.capture());
        assertThat(savedJob.getValue().status()).isEqualTo(IndexingJobStatus.FAILED);
        assertThat(savedJob.getValue().nextAttemptAt()).isNull();
    }

    private Track track(TrackStatus status) {
        Instant now = Instant.now();
        return new Track(
                UUID.randomUUID(),
                "Track",
                "Artist",
                "Album",
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
                null,
                null,
                null,
                1_000L,
                "audio/track",
                "checksum",
                status,
                now,
                now);
    }

    private IndexingJob job(UUID trackId, IndexingJobStatus status, int attempts) {
        return new IndexingJob(
                UUID.randomUUID(), trackId, status, 10, attempts, null, null, Instant.now(), Instant.now(), null, null);
    }
}
