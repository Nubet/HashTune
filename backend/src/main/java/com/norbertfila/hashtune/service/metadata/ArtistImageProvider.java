package com.norbertfila.hashtune.service.metadata;

import java.util.Optional;

public interface ArtistImageProvider {
    Optional<String> findArtistImage(String artistName);
}
