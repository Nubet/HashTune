package com.norbertfila.hashtune.service.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.norbertfila.hashtune.service.validation.ArtistNameNormalizer;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeezerArtistImageAdapter implements ArtistImageProvider {
    private final RestClient deezerClient;

    @Override
    public Optional<String> findArtistImage(String artistName) {
        if (artistName == null || artistName.isBlank()) {
            return Optional.empty();
        }

        URI uri = UriComponentsBuilder.fromPath("/search/artist")
                .queryParam("q", artistName)
                .queryParam("limit", 10)
                .build()
                .toUri();

        try {
            DeezerArtistSearchResponse response =
                    deezerClient.get().uri(uri).retrieve().body(DeezerArtistSearchResponse.class);
            return response == null
                    ? Optional.empty()
                    : response.data().stream()
                            .filter(result -> matches(result, artistName))
                            .map(DeezerArtist::pictureXl)
                            .filter(url -> url != null && !url.isBlank())
                            .findFirst();
        } catch (RestClientException exception) {
            log.debug("Could not resolve artist image from Deezer for {}", artistName, exception);
            return Optional.empty();
        }
    }

    private boolean matches(DeezerArtist result, String requestedName) {
        return result != null
                && result.name() != null
                && ArtistNameNormalizer.normalize(result.name()).equals(ArtistNameNormalizer.normalize(requestedName));
    }

    private record DeezerArtistSearchResponse(List<DeezerArtist> data) {
        private DeezerArtistSearchResponse {
            data = data == null ? List.of() : data;
        }
    }

    private record DeezerArtist(
            String name, @JsonProperty("picture_xl") String pictureXl) {}
}
