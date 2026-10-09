package com.norbertfila.hashtune.repository.artist;

import java.util.Optional;

public interface ArtistImageCacheRepository {
    Optional<ArtistImageCacheEntry> findByArtistKey(String artistKey);

    ArtistImageCacheEntry save(ArtistImageCacheEntry entry);
}
