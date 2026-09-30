package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.track.TrackOrigin;

public record TrackSearchQuery(String query, TrackOrigin origin, String artist, String album, int page, int size) {}
