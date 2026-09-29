package com.norbertfila.hashtune.adapter.out.fingerprinting;

import java.util.ArrayList;
import java.util.List;

final class FingerprintEncoder {
    private static final int TARGET_ZONE_SIZE = 5;

    List<FingerprintOccurrence> encode(List<SpectralPeak> peaks) {
        List<FingerprintOccurrence> fingerprints = new ArrayList<>();

        for (int anchorIndex = 0; anchorIndex < peaks.size(); anchorIndex++) {
            SpectralPeak anchor = peaks.get(anchorIndex);
            int targetEnd = Math.min(peaks.size(), anchorIndex + TARGET_ZONE_SIZE + 1);
            for (int targetIndex = anchorIndex + 1; targetIndex < targetEnd; targetIndex++) {
                fingerprints.add(new FingerprintOccurrence(
                        FingerprintHash.encode(anchor, peaks.get(targetIndex)),
                        Math.toIntExact(Math.round(anchor.timeSeconds() * 1000))));
            }
        }
        return fingerprints;
    }
}
