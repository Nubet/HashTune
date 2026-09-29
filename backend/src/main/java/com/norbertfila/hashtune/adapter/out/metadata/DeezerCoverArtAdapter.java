package com.norbertfila.hashtune.adapter.out.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.norbertfila.hashtune.application.port.out.CoverArtProvider;
import java.util.List;
import java.util.Locale;
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
public class DeezerCoverArtAdapter implements CoverArtProvider {
    private final RestClient deezerClient;

    @Override
    public Optional<String> findCoverArt(String title, String artist, String album) {
        if (isBlank(title) || isBlank(artist) || "Unknown".equalsIgnoreCase(artist)) {
            return Optional.empty();
        }

        String query = "%s %s".formatted(artist, title);
        String uri = UriComponentsBuilder.fromPath("/search")
                .queryParam("q", query)
                .queryParam("limit", 10)
                .toUriString();

        try {
            DeezerSearchResponse response =
                    deezerClient.get().uri(uri).retrieve().body(DeezerSearchResponse.class);
            return response == null
                    ? Optional.empty()
                    : response.data().stream()
                            .filter(result -> matches(result, title, artist))
                            .map(DeezerTrack::coverArtUrl)
                            .filter(url -> !isBlank(url))
                            .findFirst();
        } catch (RestClientException exception) {
            log.debug("Could not resolve cover art from Deezer for {} - {}", artist, title, exception);
            return Optional.empty();
        }
    }

    private boolean matches(DeezerTrack result, String title, String artist) {
        return result != null
                && result.artist() != null
                && normalize(result.title()).equals(normalize(title))
                && artistsMatch(result.artist().name(), artist);
    }

    static boolean artistsMatch(String resultArtist, String requestedArtist) {
        String normalizedResult = normalize(resultArtist);
        String normalizedRequested = normalize(requestedArtist);
        return normalizedResult.equals(normalizedRequested)
                || normalizedRequested.contains(normalizedResult)
                || normalizedResult.contains(normalizedRequested);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record DeezerSearchResponse(List<DeezerTrack> data) {
        private DeezerSearchResponse {
            data = data == null ? List.of() : data;
        }
    }

    private record DeezerTrack(String title, DeezerArtist artist, DeezerAlbum album) {
        private String coverArtUrl() {
            return album == null ? null : album.coverXl();
        }
    }

    private record DeezerArtist(String name) {}

    private record DeezerAlbum(@JsonProperty("cover_xl") String coverXl) {}
}
