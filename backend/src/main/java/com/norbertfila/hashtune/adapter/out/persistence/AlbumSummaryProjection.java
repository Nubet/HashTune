package com.norbertfila.hashtune.adapter.out.persistence;

import java.util.UUID;

public interface AlbumSummaryProjection {
    String getTitle();

    String getArtist();

    String getCoverArtUrl();

    UUID getCoverArtTrackId();

    long getTrackCount();
}
