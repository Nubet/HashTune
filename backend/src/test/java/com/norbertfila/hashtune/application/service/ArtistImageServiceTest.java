package com.norbertfila.hashtune.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.norbertfila.hashtune.application.port.out.ArtistImageCacheEntry;
import com.norbertfila.hashtune.application.port.out.ArtistImageCacheRepository;
import com.norbertfila.hashtune.application.port.out.ArtistImageProvider;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class ArtistImageServiceTest {
    private final ArtistImageCacheRepository cache = Mockito.mock(ArtistImageCacheRepository.class);
    private final ArtistImageProvider provider = Mockito.mock(ArtistImageProvider.class);
    private final ArtistImageService service = new ArtistImageService(cache, provider, Runnable::run);

    @Test
    void resolvesAndCachesArtistImage() {
        when(cache.findByArtistKey("josef bratan")).thenReturn(Optional.empty());
        when(provider.findArtistImage("Josef Bratan"))
                .thenReturn(Optional.of("https://cdn-images.dzcdn.net/images/artist/josef.jpg"));

        assertThat(service.findImageForDelivery(" Josef   Bratan "))
                .contains("https://cdn-images.dzcdn.net/images/artist/josef.jpg");
        verify(cache).save(argThat(entry ->
                        entry.artistKey().equals("josef bratan")
                        && entry.displayName().equals("Josef Bratan")
                        && entry.imageUrl().equals("https://cdn-images.dzcdn.net/images/artist/josef.jpg")));
    }

    @Test
    void cachesMissingImageAndDoesNotCallProviderAgainWhileFresh() {
        Instant checkedAt = Instant.now().minusSeconds(60);
        when(cache.findByArtistKey("unknown artist"))
                .thenReturn(Optional.of(new ArtistImageCacheEntry("unknown artist", "Unknown Artist", null, checkedAt)));

        assertThat(service.findImageForDelivery("Unknown Artist")).isEmpty();
        verify(provider, never()).findArtistImage("Unknown Artist");
    }

    @Test
    void doesNotResolveMissingCacheEntriesWhenListingArtists() {
        when(cache.findByArtistKey("new artist")).thenReturn(Optional.empty());

        assertThat(service.findCachedImage("New Artist")).isEmpty();
        verify(provider, never()).findArtistImage("New Artist");
    }
}
