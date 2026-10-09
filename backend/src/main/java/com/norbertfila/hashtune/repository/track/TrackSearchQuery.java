package com.norbertfila.hashtune.repository.track;

import com.norbertfila.hashtune.entity.track.TrackOrigin;

public record TrackSearchQuery(String query, TrackOrigin origin, String artist, String album, int page, int size) {}
