package com.norbertfila.hashtune.repository.track;

import java.util.UUID;

public interface AlbumSummaryProjection {
    String getTitle();

    String getArtist();

    String getCoverArtUrl();

    UUID getCoverArtTrackId();

    long getTrackCount();
}
