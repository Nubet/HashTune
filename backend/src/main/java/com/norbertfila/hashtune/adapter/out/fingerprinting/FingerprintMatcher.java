package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.application.port.out.FingerprintRepository.FingerprintMatch;
import com.norbertfila.hashtune.domain.fingerprint.FingerprintOccurrence;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

final class FingerprintMatcher {
    private static final int OFFSET_BUCKET_SIZE_MS = 100;
    private static final int MINIMUM_OFFSET_CLUSTER_SIZE = 8;
    private static final double MINIMUM_CONFIDENCE = 0.35;

    MatchResult match(List<FingerprintOccurrence> sampleFingerprints, List<FingerprintMatch> storedMatches) {
        Map<Long, List<Integer>> sampleOffsetsByHash = sampleFingerprints.stream()
                .collect(Collectors.groupingBy(
                        FingerprintOccurrence::hash,
                        HashMap::new,
                        Collectors.mapping(FingerprintOccurrence::anchorOffsetMs, Collectors.toList())));

        Map<UUID, Map<Long, Integer>> offsetCountsByTrack = new HashMap<>();
        Map<UUID, Integer> totalMatchesByTrack = new HashMap<>();
        for (FingerprintMatch storedMatch : storedMatches) {
            List<Integer> sampleOffsets = sampleOffsetsByHash.get(storedMatch.hash());
            if (sampleOffsets == null) {
                continue;
            }
            for (int sampleOffset : sampleOffsets) {
                long offset = storedMatch.anchorOffsetMs() - (long) sampleOffset;
                long bucket = Math.floorDiv(offset, OFFSET_BUCKET_SIZE_MS);
                offsetCountsByTrack
                        .computeIfAbsent(storedMatch.trackId(), ignored -> new HashMap<>())
                        .merge(bucket, 1, Integer::sum);
                totalMatchesByTrack.merge(storedMatch.trackId(), 1, Integer::sum);
            }
        }

        return offsetCountsByTrack.entrySet().stream()
                .map(entry -> bestTrackMatch(
                        entry.getKey(),
                        entry.getValue(),
                        totalMatchesByTrack.get(entry.getKey()),
                        sampleFingerprints.size()))
                .filter(result -> result.offsetClusterSize() >= MINIMUM_OFFSET_CLUSTER_SIZE)
                .filter(result -> result.confidence() >= MINIMUM_CONFIDENCE)
                .max(Comparator.comparingInt(MatchResult::offsetClusterSize))
                .orElse(MatchResult.noMatch());
    }

    private MatchResult bestTrackMatch(
            UUID trackId, Map<Long, Integer> offsetCounts, int totalMatches, int sampleFingerprintCount) {
        Map.Entry<Long, Integer> bestBucket = offsetCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        int clusterSize = bestBucket.getValue();
        double confidence = Math.min(1, clusterSize / (double) Math.max(1, sampleFingerprintCount));
        return new MatchResult(
                trackId, bestBucket.getKey() * OFFSET_BUCKET_SIZE_MS, confidence, totalMatches, clusterSize);
    }

    record MatchResult(UUID trackId, long matchedAtMs, double confidence, int hashMatches, int offsetClusterSize) {
        static MatchResult noMatch() {
            return new MatchResult(null, 0, 0, 0, 0);
        }
    }
}
