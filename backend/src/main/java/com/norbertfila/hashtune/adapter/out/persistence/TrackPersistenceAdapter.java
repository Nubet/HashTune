package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.application.port.out.TrackRepository;
import com.norbertfila.hashtune.domain.track.Track;
import com.norbertfila.hashtune.domain.track.TrackStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackPersistenceAdapter implements TrackRepository {
    private final SpringDataTrackRepository repository;

    @Override
    public Track save(Track track) {
        return toDomain(repository.save(toEntity(track)));
    }

    @Override
    public Optional<Track> findById(UUID id) {
        return repository.findById(id).map(TrackPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Track> findByChecksum(String checksum) {
        return repository.findByChecksum(checksum).map(TrackPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Track> findFirstByStatus(TrackStatus status) {
        return repository.findFirstByStatusOrderByCreatedAtAsc(status).map(TrackPersistenceAdapter::toDomain);
    }

    @Override
    public List<Track> search(String query, int limit, int offset) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Page<TrackEntity> result = normalizedQuery == null
                ? repository.findAll(PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                : repository.searchByQuery(
                        normalizedQuery,
                        PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        return result.getContent().stream()
                .map(TrackPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public void delete(Track track) {
        repository.delete(toEntity(track));
    }

    private static TrackEntity toEntity(Track track) {
        return TrackEntity.builder()
                .id(track.id())
                .title(track.title())
                .artist(track.artist())
                .album(track.album())
                .albumArtist(track.albumArtist())
                .composer(track.composer())
                .genre(track.genre())
                .releaseYear(track.releaseYear())
                .trackNumber(track.trackNumber())
                .discNumber(track.discNumber())
                .isrc(track.isrc())
                .barcode(track.barcode())
                .comment(track.comment())
                .coverArtUrl(track.coverArtUrl())
                .coverArtObjectKey(track.coverArtObjectKey())
                .coverArtMimeType(track.coverArtMimeType())
                .durationMs(track.durationMs())
                .audioObjectKey(track.audioObjectKey())
                .checksum(track.checksum())
                .status(track.status())
                .createdAt(track.createdAt())
                .updatedAt(track.updatedAt())
                .build();
    }

    private static Track toDomain(TrackEntity track) {
        return new Track(
                track.getId(),
                track.getTitle(),
                track.getArtist(),
                track.getAlbum(),
                track.getAlbumArtist(),
                track.getComposer(),
                track.getGenre(),
                track.getReleaseYear(),
                track.getTrackNumber(),
                track.getDiscNumber(),
                track.getIsrc(),
                track.getBarcode(),
                track.getComment(),
                track.getCoverArtUrl(),
                track.getCoverArtObjectKey(),
                track.getCoverArtMimeType(),
                track.getDurationMs(),
                track.getAudioObjectKey(),
                track.getChecksum(),
                track.getStatus(),
                track.getCreatedAt(),
                track.getUpdatedAt());
    }
}
