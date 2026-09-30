package com.norbertfila.hashtune.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.norbertfila.hashtune.adapter.out.metadata.AudioMetadataReader;
import com.norbertfila.hashtune.application.port.out.CoverArtProvider;
import com.norbertfila.hashtune.application.port.out.FingerprintRepository;
import com.norbertfila.hashtune.application.port.out.IndexingJobRepository;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.application.port.out.TrackRepository;
import com.norbertfila.hashtune.configuration.AudioProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class TrackApplicationServiceTest {
    private final TrackRepository tracks = mock(TrackRepository.class);
    private final IndexingJobRepository jobs = mock(IndexingJobRepository.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final FingerprintRepository fingerprints = mock(FingerprintRepository.class);
    private final StorageProperties storageProperties = new StorageProperties();
    private final AudioProperties audioProperties = new AudioProperties();
    private final AudioMetadataReader metadataReader = mock(AudioMetadataReader.class);
    private final CoverArtProvider coverArtProvider = mock(CoverArtProvider.class);
    private TrackApplicationService service;

    @BeforeEach
    void setUp() {
        storageProperties.setAudioBucket("audio");
        storageProperties.setTempBucket("temp");
        audioProperties.setMaxFileSizeBytes(10_000);
        service = new TrackApplicationService(
                tracks,
                jobs,
                storage,
                fingerprints,
                storageProperties,
                audioProperties,
                metadataReader,
                coverArtProvider);
        when(jobs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void cleansUploadedObjectsWhenDatabaseSaveFails() {
        MockMultipartFile file = audioFile("track.mp3");
        when(metadataReader.read(file)).thenReturn(emptyMetadata());
        when(tracks.save(any())).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> service.importTrack(file, TrackOrigin.PERSONAL, null))
                .isInstanceOf(IllegalStateException.class);

        verify(storage).delete(eq("audio"), startsWith("audio/"));
    }

    private MockMultipartFile audioFile(String name) {
        return new MockMultipartFile("file", name, "audio/mpeg", new byte[] {1, 2, 3});
    }

    private AudioMetadataReader.AudioMetadata emptyMetadata() {
        return new AudioMetadataReader.AudioMetadata(
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
