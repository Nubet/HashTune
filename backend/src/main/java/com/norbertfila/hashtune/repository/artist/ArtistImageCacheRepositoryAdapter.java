package com.norbertfila.hashtune.repository.artist;

import com.norbertfila.hashtune.entity.health.ArtistImageCacheEntity;
import com.norbertfila.hashtune.repository.artist.ArtistImageCacheEntry;
import com.norbertfila.hashtune.repository.artist.ArtistImageCacheRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArtistImageCacheRepositoryAdapter implements ArtistImageCacheRepository {
    private final SpringDataArtistImageCacheRepository repository;

    @Override
    public Optional<ArtistImageCacheEntry> findByArtistKey(String artistKey) {
        return repository.findById(artistKey).map(ArtistImageCacheRepositoryAdapter::toDomain);
    }

    @Override
    public ArtistImageCacheEntry save(ArtistImageCacheEntry entry) {
        return toDomain(repository.save(toEntity(entry)));
    }

    private static ArtistImageCacheEntity toEntity(ArtistImageCacheEntry entry) {
        return ArtistImageCacheEntity.builder()
                .artistKey(entry.artistKey())
                .displayName(entry.displayName())
                .imageUrl(entry.imageUrl())
                .checkedAt(entry.checkedAt())
                .build();
    }

    private static ArtistImageCacheEntry toDomain(ArtistImageCacheEntity entity) {
        return new ArtistImageCacheEntry(
                entity.getArtistKey(), entity.getDisplayName(), entity.getImageUrl(), entity.getCheckedAt());
    }
}
