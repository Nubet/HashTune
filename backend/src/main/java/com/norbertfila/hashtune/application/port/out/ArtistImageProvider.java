package com.norbertfila.hashtune.application.port.out;

import java.util.Optional;

public interface ArtistImageProvider {
    Optional<String> findArtistImage(String artistName);
}
