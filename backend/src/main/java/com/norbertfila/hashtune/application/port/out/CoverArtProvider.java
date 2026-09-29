package com.norbertfila.hashtune.application.port.out;

import java.util.Optional;

public interface CoverArtProvider {
    Optional<String> findCoverArt(String title, String artist, String album);
}
