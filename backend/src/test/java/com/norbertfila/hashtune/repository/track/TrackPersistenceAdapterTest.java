package com.norbertfila.hashtune.repository.track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class TrackPersistenceAdapterTest {
    private final SpringDataTrackRepository repository = mock(SpringDataTrackRepository.class);
    private final TrackPersistenceAdapter adapter = new TrackPersistenceAdapter(repository);

    @Test
    void mapsAlbumArtistProjectionToAlbumSummaryArtist() {
        AlbumSummaryProjection projection = mock(AlbumSummaryProjection.class);
        when(projection.getTitle()).thenReturn("100 dni do matury");
        when(projection.getArtist()).thenReturn("Album Artist");
        when(projection.getCoverArtUrl()).thenReturn("https://example.com/cover.jpg");
        when(projection.getCoverArtTrackId()).thenReturn(UUID.randomUUID());
        when(projection.getTrackCount()).thenReturn(4L);
        when(repository.searchAlbums(eq("100 dni do matury"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 25), 1));

        var result = adapter.searchAlbums("100 dni do matury", null, 0, 25);

        assertThat(result.content()).singleElement().satisfies(album -> {
            assertThat(album.title()).isEqualTo("100 dni do matury");
            assertThat(album.artist()).isEqualTo("Album Artist");
            assertThat(album.trackCount()).isEqualTo(4L);
        });
    }
}
