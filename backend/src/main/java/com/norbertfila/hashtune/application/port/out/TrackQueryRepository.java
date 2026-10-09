package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.entity.track.Track;
import com.norbertfila.hashtune.entity.track.TrackOrigin;

public interface TrackQueryRepository {
    PageResult<Track> searchTracks(TrackSearchQuery query);

    PageResult<AlbumSummary> searchAlbums(String query, TrackOrigin origin, int page, int size);

    PageResult<ArtistSummary> searchArtists(String query, TrackOrigin origin, int page, int size);
}
