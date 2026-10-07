package com.norbertfila.hashtune.application.port.out;

import java.util.Optional;

public interface ArtistImageCacheRepository {
    Optional<ArtistImageCacheEntry> findByArtistKey(String artistKey);

    ArtistImageCacheEntry save(ArtistImageCacheEntry entry);
}
