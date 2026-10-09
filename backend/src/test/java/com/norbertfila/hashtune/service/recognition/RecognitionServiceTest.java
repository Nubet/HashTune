package com.norbertfila.hashtune.service.recognition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import com.norbertfila.hashtune.entity.track.Track;
import com.norbertfila.hashtune.entity.track.TrackStatus;
import com.norbertfila.hashtune.repository.recognition.RecognitionRepository;
import com.norbertfila.hashtune.repository.track.TrackRepository;
import com.norbertfila.hashtune.security.RecognitionConcurrencyLimiter;
import com.norbertfila.hashtune.service.fingerprint.AudioRecognitionEngine;
import com.norbertfila.hashtune.service.storage.ObjectStoragePort;
import com.norbertfila.hashtune.service.validation.AudioUploadValidator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class RecognitionServiceTest {
    private final RecognitionRepository recognitions = mock(RecognitionRepository.class);
    private final RecognitionConcurrencyLimiter concurrencyLimiter = mock(RecognitionConcurrencyLimiter.class);
    private final TrackRepository tracks = mock(TrackRepository.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final AudioRecognitionEngine engine = mock(AudioRecognitionEngine.class);
    private final StorageProperties storageProperties = new StorageProperties();
    private final AudioSafetyProperties audioSafetyProperties = new AudioSafetyProperties();
    private RecognitionService service;
    private final ExternalIdentity owner = new ExternalIdentity("test-issuer", "test-subject");

    @BeforeEach
    void setUp() {
        storageProperties.setTempBucket("temp");
        storageProperties.setAudioBucket("audio");
        audioSafetyProperties.setMaxFileSizeBytes(10_000);
        service = new RecognitionService(
                recognitions,
                concurrencyLimiter,
                tracks,
                storage,
                engine,
                storageProperties,
                new AudioUploadValidator(audioSafetyProperties));
        when(concurrencyLimiter.tryAcquire(owner)).thenReturn(true);
    }

    @Test
    void cleansTemporarySampleWhenRecognitionFails() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.mp3", "audio/mpeg", new byte[] {1, 2, 3});
        when(engine.recognize(any())).thenThrow(new IllegalStateException("decoder failed"));

        assertThatThrownBy(() -> service.probe(
                        owner, file, com.norbertfila.hashtune.entity.recognition.RecognitionSource.AUDIO_FILE))
                .isInstanceOf(IllegalStateException.class);

        verify(storage).delete(eq("temp"), startsWith("samples/"));
    }

    @Test
    void persistsMatchedProbeForHistory() {
        UUID trackId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "sample.mp3", "audio/mpeg", new byte[] {1, 2, 3});
        Track track = new Track(
                trackId,
                "Remember the Time",
                "Michael Jackson",
                "Dangerous",
                null,
                null,
                null,
                null,
                TrackStatus.INDEXED,
                Instant.now(),
                Instant.now());
        when(engine.recognize(any()))
                .thenReturn(new AudioRecognitionEngine.RecognitionResult(true, trackId, 1.0, 27_000, 5_000, 1, 1));
        when(tracks.findById(trackId)).thenReturn(Optional.of(track));

        service.probe(owner, file, com.norbertfila.hashtune.entity.recognition.RecognitionSource.MICROPHONE);

        verify(recognitions).save(any());
    }

    @Test
    void doesNotStoreAudioFileAsMicrophoneRecording() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.mp3", "audio/mpeg", new byte[] {1, 2, 3});
        when(engine.recognize(any()))
                .thenReturn(new AudioRecognitionEngine.RecognitionResult(false, null, 0.0, 0L, 5_000, 1, 1));

        service.recognize(owner, file, com.norbertfila.hashtune.entity.recognition.RecognitionSource.AUDIO_FILE);

        verify(storage, never()).put(eq("audio"), any(), any(), anyLong(), any());
    }

    @Test
    void scopesHistoryToTheCurrentSession() {
        service.history(owner, 25, 0);

        verify(recognitions).findLatest(owner, 25, 0);
    }

    @Test
    void clearsOnlyTheCurrentSessionHistory() {
        when(recognitions.findAll(owner)).thenReturn(List.of());

        service.clearHistory(owner);

        verify(recognitions).findAll(owner);
        verify(recognitions).deleteAll(owner);
    }
}
