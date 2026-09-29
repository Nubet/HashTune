package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.fingerprint.FingerprintOccurrence;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FingerprintRepository {
    void replace(UUID trackId, List<FingerprintOccurrence> fingerprints);

    long countByTrackId(UUID trackId);

    List<FingerprintMatch> findMatches(Collection<Long> hashes);

    void deleteByTrackId(UUID trackId);

    record FingerprintMatch(long hash, UUID trackId, int anchorOffsetMs) {}
}
