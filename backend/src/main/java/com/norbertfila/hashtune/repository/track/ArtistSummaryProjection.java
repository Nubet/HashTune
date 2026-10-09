package com.norbertfila.hashtune.repository.track;

public interface ArtistSummaryProjection {
    String getName();

    long getTrackCount();

    long getAlbumCount();
}
