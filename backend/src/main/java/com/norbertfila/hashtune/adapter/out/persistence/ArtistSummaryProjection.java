package com.norbertfila.hashtune.adapter.out.persistence;

public interface ArtistSummaryProjection {
    String getName();

    long getTrackCount();

    long getAlbumCount();
}
