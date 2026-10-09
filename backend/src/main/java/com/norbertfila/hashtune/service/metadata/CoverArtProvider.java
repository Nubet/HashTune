package com.norbertfila.hashtune.service.metadata;

import java.util.Optional;

public interface CoverArtProvider {
    Optional<String> findCoverArt(String title, String artist, String album);
}
