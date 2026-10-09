package com.norbertfila.hashtune.repository.artist;

import java.time.Instant;

public record ArtistImageCacheEntry(String artistKey, String displayName, String imageUrl, Instant checkedAt) {}
