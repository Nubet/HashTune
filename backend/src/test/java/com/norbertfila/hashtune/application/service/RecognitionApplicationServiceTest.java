package com.norbertfila.hashtune.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.norbertfila.hashtune.application.port.out.AudioRecognitionEngine;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.application.port.out.RecognitionRepository;
import com.norbertfila.hashtune.application.port.out.TrackRepository;
import com.norbertfila.hashtune.configuration.AudioProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class RecognitionApplicationServiceTest {
    private final RecognitionRepository recognitions = mock(RecognitionRepository.class);
    private final TrackRepository tracks = mock(TrackRepository.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final AudioRecognitionEngine engine = mock(AudioRecognitionEngine.class);
    private final StorageProperties storageProperties = new StorageProperties();
    private final AudioProperties audioProperties = new AudioProperties();
    private RecognitionApplicationService service;

    @BeforeEach
    void setUp() {
        storageProperties.setTempBucket("temp");
        audioProperties.setMaxFileSizeBytes(10_000);
        service = new RecognitionApplicationService(
                recognitions, tracks, storage, engine, storageProperties, audioProperties);
    }

    @Test
    void cleansTemporarySampleWhenRecognitionFails() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.mp3", "audio/mpeg", new byte[] {1, 2, 3});
        when(engine.recognize(any())).thenThrow(new IllegalStateException("decoder failed"));

        assertThatThrownBy(() ->
                        service.probe(file, com.norbertfila.hashtune.domain.recognition.RecognitionSource.AUDIO_FILE))
                .isInstanceOf(IllegalStateException.class);

        verify(storage).delete(eq("temp"), startsWith("samples/"));
    }
}
