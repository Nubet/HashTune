package com.norbertfila.hashtune.application.service;

import com.norbertfila.hashtune.application.port.out.AlbumSummary;
import com.norbertfila.hashtune.application.port.out.ArtistSummary;
import com.norbertfila.hashtune.application.port.out.PageResult;
import com.norbertfila.hashtune.application.port.out.TrackQueryRepository;
import com.norbertfila.hashtune.application.port.out.TrackSearchQuery;
import com.norbertfila.hashtune.domain.track.Track;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackQueryService {
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;

    private final TrackQueryRepository tracks;
    private final ArtistImageService artistImages;

    public PageResult<Track> searchTracks(TrackSearchQuery query) {
        return tracks.searchTracks(normalize(query));
    }

    public PageResult<AlbumSummary> searchAlbums(String query, TrackOrigin origin, int page, int size) {
        return tracks.searchAlbums(normalizeQuery(query), origin, normalizePage(page), normalizeSize(size));
    }

    public PageResult<ArtistSummary> searchArtists(String query, TrackOrigin origin, int page, int size) {
        PageResult<ArtistSummary> result =
                tracks.searchArtists(normalizeQuery(query), origin, normalizePage(page), normalizeSize(size));
        return new PageResult<>(
                result.content().stream()
                        .map(artist -> new ArtistSummary(
                                artist.name(),
                                artist.trackCount(),
                                artist.albumCount(),
                                artistImages.findCachedImage(artist.name()).orElse(null)))
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements());
    }

    private TrackSearchQuery normalize(TrackSearchQuery query) {
        return new TrackSearchQuery(
                normalizeQuery(query.query()),
                query.origin(),
                normalizeQuery(query.artist()),
                normalizeQuery(query.album()),
                normalizePage(query.page()),
                normalizeSize(query.size()));
    }

    private String normalizeQuery(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
    }
}
