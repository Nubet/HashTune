package com.norbertfila.hashtune.adapter.out.fingerprinting;

import static org.assertj.core.api.Assertions.assertThat;

import com.norbertfila.hashtune.application.port.out.FingerprintRepository.FingerprintMatch;
import com.norbertfila.hashtune.domain.fingerprint.FingerprintOccurrence;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FingerprintMatcherTest {
    private final FingerprintMatcher matcher = new FingerprintMatcher();

    @Test
    void selectsTrackWithTheStrongestTimeAlignedCluster() {
        UUID matchingTrack = UUID.randomUUID();
        UUID unrelatedTrack = UUID.randomUUID();
        List<FingerprintOccurrence> sample = List.of(
                new FingerprintOccurrence(10, 1_000),
                new FingerprintOccurrence(20, 2_000),
                new FingerprintOccurrence(30, 3_000));
        List<FingerprintMatch> stored = List.of(
                new FingerprintMatch(10, matchingTrack, 11_000),
                new FingerprintMatch(20, matchingTrack, 12_000),
                new FingerprintMatch(30, matchingTrack, 13_000),
                new FingerprintMatch(10, unrelatedTrack, 5_000),
                new FingerprintMatch(20, unrelatedTrack, 9_000));

        FingerprintMatcher.MatchResult result = matcher.match(sample, stored);

        assertThat(result.trackId()).isEqualTo(matchingTrack);
        assertThat(result.matchedAtMs()).isEqualTo(10_000);
        assertThat(result.hashMatches()).isEqualTo(3);
        assertThat(result.offsetClusterSize()).isEqualTo(3);
    }

    @Test
    void returnsNoMatchWhenThereIsNoReliableOffsetCluster() {
        UUID trackId = UUID.randomUUID();
        List<FingerprintOccurrence> sample = List.of(new FingerprintOccurrence(10, 1_000));
        List<FingerprintMatch> stored = List.of(new FingerprintMatch(10, trackId, 11_000));

        FingerprintMatcher.MatchResult result = matcher.match(sample, stored);

        assertThat(result.trackId()).isNull();
    }
}
