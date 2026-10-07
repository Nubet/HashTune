package com.norbertfila.hashtune.application.service;

import com.norbertfila.hashtune.application.port.out.ArtistImageCacheEntry;
import com.norbertfila.hashtune.application.port.out.ArtistImageCacheRepository;
import com.norbertfila.hashtune.application.port.out.ArtistImageProvider;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ArtistImageService {
    private static final Duration CACHE_TTL = Duration.ofDays(30);
    private static final String DEEZER_IMAGE_HOST = "cdn-images.dzcdn.net";

    private final ArtistImageCacheRepository cache;
    private final ArtistImageProvider provider;
    private final Executor refreshExecutor;
    private final ConcurrentHashMap<String, CompletableFuture<Optional<String>>> refreshes = new ConcurrentHashMap<>();

    public ArtistImageService(
            ArtistImageCacheRepository cache,
            ArtistImageProvider provider,
            @Qualifier("artistImageExecutor") Executor refreshExecutor) {
        this.cache = cache;
        this.provider = provider;
        this.refreshExecutor = refreshExecutor;
    }

    public Optional<String> findCachedImage(String artistName) {
        ArtistName name = normalize(artistName);
        if (name.key().isBlank()) {
            return Optional.empty();
        }

        Optional<ArtistImageCacheEntry> cached = cache.findByArtistKey(name.key());
        if (cached.isEmpty()) {
            return Optional.empty();
        }

        if (isFresh(cached.get())) {
            return cached.flatMap(this::trustedImageUrl);
        }

        refreshAsync(name);
        return cached.flatMap(this::trustedImageUrl);
    }

    public Optional<String> findImageForDelivery(String artistName) {
        ArtistName name = normalize(artistName);
        if (name.key().isBlank()) {
            return Optional.empty();
        }

        Optional<ArtistImageCacheEntry> cached = cache.findByArtistKey(name.key());
        if (cached.isPresent() && isFresh(cached.get())) {
            return cached.flatMap(this::trustedImageUrl);
        }

        if (cached.flatMap(this::trustedImageUrl).isPresent()) {
            refreshAsync(name);
            return cached.flatMap(this::trustedImageUrl);
        }

        try {
            return refresh(name).join();
        } catch (CompletionException exception) {
            return Optional.empty();
        }
    }

    private CompletableFuture<Optional<String>> refresh(ArtistName name) {
        return getOrStartRefresh(name);
    }

    private void refreshAsync(ArtistName name) {
        getOrStartRefresh(name);
    }

    private CompletableFuture<Optional<String>> getOrStartRefresh(ArtistName name) {
        return refreshes.computeIfAbsent(name.key(), ignored -> {
            CompletableFuture<Optional<String>> created =
                    CompletableFuture.supplyAsync(() -> resolve(name), refreshExecutor);
            created.whenComplete((result, error) -> refreshes.remove(name.key(), created));
            return created;
        });
    }

    private Optional<String> resolve(ArtistName name) {
        Instant now = Instant.now();
        String imageUrl = provider.findArtistImage(name.displayName())
                .filter(this::isTrustedImageUrl)
                .orElse(null);
        cache.save(new ArtistImageCacheEntry(name.key(), name.displayName(), imageUrl, now));
        return Optional.ofNullable(imageUrl);
    }

    private boolean isFresh(ArtistImageCacheEntry entry) {
        return entry.checkedAt().plus(CACHE_TTL).isAfter(Instant.now());
    }

    private boolean isTrustedImageUrl(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);
            return "https".equalsIgnoreCase(uri.getScheme()) && DEEZER_IMAGE_HOST.equalsIgnoreCase(uri.getHost());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private Optional<String> trustedImageUrl(ArtistImageCacheEntry entry) {
        return Optional.ofNullable(entry.imageUrl()).filter(this::isTrustedImageUrl);
    }

    private ArtistName normalize(String artistName) {
        // Collapse repeated spaces so lookup and cache use the same artist name
        String displayName = artistName == null ? "" : artistName.trim().replaceAll("\\s+", " ");
        String key = ArtistNameNormalizer.normalize(displayName);
        return new ArtistName(displayName, key);
    }

    private record ArtistName(String displayName, String key) {}
}
