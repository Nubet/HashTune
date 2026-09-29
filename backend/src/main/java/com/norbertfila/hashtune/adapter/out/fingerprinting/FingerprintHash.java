package com.norbertfila.hashtune.adapter.out.fingerprinting;

final class FingerprintHash {
    private static final int FREQUENCY_BUCKET_SIZE_HZ = 10;
    private static final int FREQUENCY_BITS = 9;
    private static final int TIME_DELTA_BITS = 14;
    private static final int TARGET_FREQUENCY_SHIFT = TIME_DELTA_BITS;
    private static final int ANCHOR_FREQUENCY_SHIFT = FREQUENCY_BITS + TARGET_FREQUENCY_SHIFT;
    private static final int MILLISECONDS_PER_SECOND = 1_000;
    private static final long FREQUENCY_MASK = (1L << FREQUENCY_BITS) - 1;
    private static final long TIME_DELTA_MASK = (1L << TIME_DELTA_BITS) - 1;

    private FingerprintHash() {}

    static long encode(SpectralPeak anchor, SpectralPeak target) {
        long anchorFrequencyBucket = frequencyBucket(anchor.frequencyHz());
        long targetFrequencyBucket = frequencyBucket(target.frequencyHz());
        long timeDeltaMilliseconds =
                Math.round((target.timeSeconds() - anchor.timeSeconds()) * MILLISECONDS_PER_SECOND);

        return pack(anchorFrequencyBucket, targetFrequencyBucket, timeDeltaMilliseconds);
    }

    private static long frequencyBucket(double frequencyHz) {
        return ((long) (frequencyHz / FREQUENCY_BUCKET_SIZE_HZ)) & FREQUENCY_MASK;
    }

    private static long pack(long anchorFrequencyBucket, long targetFrequencyBucket, long timeDeltaMilliseconds) {
        // 10 Hz buckets cover the 5 kHz input range with 9 bits; 14 time bits cover 16.384 seconds
        return ((anchorFrequencyBucket & FREQUENCY_MASK) << ANCHOR_FREQUENCY_SHIFT)
                | ((targetFrequencyBucket & FREQUENCY_MASK) << TARGET_FREQUENCY_SHIFT)
                | (timeDeltaMilliseconds & TIME_DELTA_MASK);
    }
}
