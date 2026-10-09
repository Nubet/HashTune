package com.norbertfila.hashtune.mapper;

import com.norbertfila.hashtune.dto.response.AlbumResponse;
import com.norbertfila.hashtune.dto.response.ArtistResponse;
import com.norbertfila.hashtune.dto.response.PageResponse;
import com.norbertfila.hashtune.dto.response.ReindexAllResponse;
import com.norbertfila.hashtune.entity.track.Track;
import com.norbertfila.hashtune.repository.track.AlbumSummary;
import com.norbertfila.hashtune.repository.track.ArtistSummary;
import com.norbertfila.hashtune.repository.track.PageResult;
import com.norbertfila.hashtune.service.track.TrackService;

public final class LibraryMapper {
    private LibraryMapper() {}

    public static PageResponse<com.norbertfila.hashtune.dto.response.TrackResponse> tracks(PageResult<Track> result) {
        return page(result, TrackMapper::toResponse);
    }

    public static PageResponse<AlbumResponse> albums(PageResult<AlbumSummary> result) {
        return page(result, album -> new AlbumResponse(
                album.title(),
                album.artist(),
                album.coverArtUrl() == null && album.coverArtTrackId() != null
                        ? TrackMapper.coverUrl(album.coverArtTrackId())
                        : album.coverArtUrl(),
                album.trackCount()));
    }

    public static PageResponse<ArtistResponse> artists(PageResult<ArtistSummary> result) {
        return page(result, artist -> new ArtistResponse(
                artist.name(), artist.trackCount(), artist.albumCount(), artist.imageUrl()));
    }

    public static ReindexAllResponse reindexAll(TrackService.ReindexAllResult result) {
        return new ReindexAllResponse(result.scheduled(), result.alreadyProcessing(), result.awaitingConfirmation());
    }

    private static <T, R> PageResponse<R> page(PageResult<T> result, java.util.function.Function<T, R> mapper) {
        return new PageResponse<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious());
    }
}
