package com.norbertfila.hashtune.application.port.out;

import java.util.UUID;

public record AlbumSummary(String title, String artist, String coverArtUrl, UUID coverArtTrackId, long trackCount) {}
