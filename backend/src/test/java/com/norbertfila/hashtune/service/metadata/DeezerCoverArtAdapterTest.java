package com.norbertfila.hashtune.service.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeezerCoverArtAdapterTest {
    @Test
    void matchesArtistWhenProviderReturnsOneOfSeveralCreditedArtists() {
        assertThat(DeezerCoverArtAdapter.artistsMatch("Rosa Walton", "Rosa Walton & Hallie Coggins"))
                .isTrue();
    }
}
