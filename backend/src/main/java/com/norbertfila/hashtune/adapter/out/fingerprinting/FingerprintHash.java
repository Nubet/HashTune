package com.norbertfila.hashtune.adapter.out.fingerprinting;

final class FingerprintHash {
    private static final int FREQUENCY_BITS = 10;
    private static final int TIME_DELTA_BITS = 10;
    private static final int TIME_BUCKET_MS = 23;
    private static final int TARGET_FREQUENCY_SHIFT = FREQUENCY_BITS;
    private static final int TIME_DELTA_SHIFT = FREQUENCY_BITS * 2;
    private static final int MILLISECONDS_PER_SECOND = 1_000;
    private static final long FREQUENCY_MASK = (1L << FREQUENCY_BITS) - 1;
    private static final long TIME_DELTA_MASK = (1L << TIME_DELTA_BITS) - 1;

    private FingerprintHash() {}

    static long encode(SpectralPeak anchor, SpectralPeak target) {
        long anchorFrequencyBucket = anchor.frequencyBin();
        long targetFrequencyBucket = target.frequencyBin();
        long timeDeltaMilliseconds = Math.round(
                (target.timeSeconds() - anchor.timeSeconds()) * MILLISECONDS_PER_SECOND / (double) TIME_BUCKET_MS);

        return pack(anchorFrequencyBucket, targetFrequencyBucket, timeDeltaMilliseconds);
    }

    private static long pack(long anchorFrequencyBucket, long targetFrequencyBucket, long timeDeltaMilliseconds) {
        // FFT-bin frequency indices and 23 ms time buckets fit the 30-bit Wang-style hash.
        return (anchorFrequencyBucket & FREQUENCY_MASK)
                | ((targetFrequencyBucket & FREQUENCY_MASK) << TARGET_FREQUENCY_SHIFT)
                | ((timeDeltaMilliseconds & TIME_DELTA_MASK) << TIME_DELTA_SHIFT);
    }
}
