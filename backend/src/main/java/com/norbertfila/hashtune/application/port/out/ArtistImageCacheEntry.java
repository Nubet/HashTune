package com.norbertfila.hashtune.application.port.out;

import java.time.Instant;

public record ArtistImageCacheEntry(
        String artistKey, String displayName, String imageUrl, Instant checkedAt) {}
