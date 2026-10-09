package com.norbertfila.hashtune.service.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.norbertfila.hashtune.configuration.MtgJamendoSeedProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.repository.fingerprint.FingerprintRepository;
import com.norbertfila.hashtune.repository.indexing.IndexingJobRepository;
import com.norbertfila.hashtune.repository.track.TrackRepository;
import com.norbertfila.hashtune.service.metadata.CoverArtProvider;
import com.norbertfila.hashtune.service.storage.ObjectStoragePort;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

class MtgJamendoSeederTest {
    @TempDir
    Path temporaryDirectory;

    private MtgJamendoSeeder seeder;
    private TrackRepository tracks;
    private IndexingJobRepository jobs;
    private ObjectStoragePort storage;
    private CoverArtProvider coverArtProvider;
    private FingerprintRepository fingerprints;

    @BeforeEach
    void setUp() {
        MtgJamendoSeedProperties properties = new MtgJamendoSeedProperties();
        properties.setDirectory(temporaryDirectory.toString());
        tracks = mock(TrackRepository.class);
        jobs = mock(IndexingJobRepository.class);
        storage = mock(ObjectStoragePort.class);
        coverArtProvider = mock(CoverArtProvider.class);
        fingerprints = mock(FingerprintRepository.class);
        seeder = new MtgJamendoSeeder(
                properties, storage, new StorageProperties(), tracks, jobs, coverArtProvider, fingerprints);
    }

    @Test
    void parsesQuotedCommasAndEscapedQuotes() {
        List<Map<String, String>> rows = MtgJamendoSeeder.parseCsv(
                "track_id,title,artist,album\n1,Song,Artist,\"Album, Vol. 1\"\n2,\"Say \"\"hello\"\"\",Artist,Album\n");

        assertThat(rows).extracting(row -> row.get("album")).containsExactly("Album, Vol. 1", "Album");
        assertThat(rows.get(1).get("title")).isEqualTo("Say \"hello\"");
    }

    @Test
    void skipsSeedingWhenManifestIsMissing() throws Exception {
        seeder.run(new DefaultApplicationArguments());

        verifyNoInteractions(tracks, jobs, storage, coverArtProvider, fingerprints);
    }
}
