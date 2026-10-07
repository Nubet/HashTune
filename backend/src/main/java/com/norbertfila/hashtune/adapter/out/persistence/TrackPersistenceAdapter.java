package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.application.port.out.AlbumSummary;
import com.norbertfila.hashtune.application.port.out.ArtistSummary;
import com.norbertfila.hashtune.application.port.out.PageResult;
import com.norbertfila.hashtune.application.port.out.TrackQueryRepository;
import com.norbertfila.hashtune.application.port.out.TrackRepository;
import com.norbertfila.hashtune.application.port.out.TrackSearchQuery;
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
public class TrackPersistenceAdapter implements TrackRepository, TrackQueryRepository {
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
    public List<Track> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .map(TrackPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<Track> findFirstByStatus(TrackStatus status) {
        return repository.findFirstByStatusOrderByCreatedAtAsc(status).map(TrackPersistenceAdapter::toDomain);
    }

    @Override
    public PageResult<Track> searchTracks(TrackSearchQuery query) {
        Page<TrackEntity> result = repository.searchTracks(
                query.query(),
                query.origin() == null ? null : query.origin().name(),
                query.artist(),
                query.album(),
                pageRequest(
                        query.page(),
                        query.size(),
                        Sort.by(Sort.Direction.ASC, "title")
                                .and(Sort.by(Sort.Direction.ASC, "artist"))
                                .and(Sort.by(Sort.Direction.ASC, "id"))));
        return new PageResult<>(
                result.getContent().stream()
                        .map(TrackPersistenceAdapter::toDomain)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    @Override
    public PageResult<AlbumSummary> searchAlbums(
            String query, com.norbertfila.hashtune.domain.track.TrackOrigin origin, int page, int size) {
        Page<AlbumSummaryProjection> result = repository.searchAlbums(
                query,
                origin == null ? null : origin.name(),
                pageRequest(page, size, Sort.by(Sort.Direction.ASC, "title")));
        return new PageResult<>(
                result.getContent().stream()
                        .map(album -> new AlbumSummary(
                                album.getTitle(),
                                album.getArtist(),
                                album.getCoverArtUrl(),
                                album.getCoverArtTrackId(),
                                album.getTrackCount()))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    @Override
    public PageResult<ArtistSummary> searchArtists(
            String query, com.norbertfila.hashtune.domain.track.TrackOrigin origin, int page, int size) {
        Page<ArtistSummaryProjection> result = repository.searchArtists(
                query,
                origin == null ? null : origin.name(),
                pageRequest(page, size, Sort.by(Sort.Direction.ASC, "name")));
        return new PageResult<>(
                result.getContent().stream()
                        .map(artist ->
                                new ArtistSummary(artist.getName(), artist.getTrackCount(), artist.getAlbumCount(), null))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(page, size, sort);
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
                .origin(track.origin())
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
                track.getOrigin(),
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
