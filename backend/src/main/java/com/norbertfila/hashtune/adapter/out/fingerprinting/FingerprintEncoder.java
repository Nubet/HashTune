package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.entity.fingerprint.FingerprintOccurrence;
import java.util.ArrayList;
import java.util.List;

final class FingerprintEncoder {
    private static final int TARGET_MINIMUM_MS = 23;
    private static final int TARGET_MAXIMUM_MS = 4_644;
    private final int fanOut;

    FingerprintEncoder() {
        this(20);
    }

    FingerprintEncoder(int fanOut) {
        if (fanOut < 1) {
            throw new IllegalArgumentException("fanOut must be positive");
        }
        this.fanOut = fanOut;
    }

    List<FingerprintOccurrence> encode(List<SpectralPeak> peaks) {
        List<FingerprintOccurrence> fingerprints = new ArrayList<>();

        for (int anchorIndex = 0; anchorIndex < peaks.size(); anchorIndex++) {
            SpectralPeak anchor = peaks.get(anchorIndex);
            int targets = 0;
            for (int targetIndex = anchorIndex + 1; targetIndex < peaks.size(); targetIndex++) {
                long deltaMs = Math.round((peaks.get(targetIndex).timeSeconds() - anchor.timeSeconds()) * 1_000);
                if (deltaMs < TARGET_MINIMUM_MS) {
                    continue;
                }
                if (deltaMs > TARGET_MAXIMUM_MS) {
                    break;
                }
                fingerprints.add(new FingerprintOccurrence(
                        FingerprintHash.encode(anchor, peaks.get(targetIndex)),
                        Math.toIntExact(Math.round(anchor.timeSeconds() * 1_000))));
                targets++;
                if (targets >= fanOut) {
                    break;
                }
            }
        }
        return fingerprints;
    }
}
