package com.norbertfila.hashtune.repository.track;

import java.util.UUID;

public record AlbumSummary(String title, String artist, String coverArtUrl, UUID coverArtTrackId, long trackCount) {}
