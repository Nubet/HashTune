package com.norbertfila.hashtune.adapter.out.fingerprinting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.norbertfila.hashtune.domain.fingerprint.FingerprintOccurrence;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpectralFingerprintingTest {
    private static final int TEST_SAMPLE_RATE = CanonicalAudioFormat.SAMPLE_RATE;
    private static final int TEST_DURATION_SECONDS = 2;

    private final SpectralFingerprinting fingerprinting = new SpectralFingerprinting();

    @Test
    void spectrumAnalyzerFindsTheExpectedFrequencyBin() {
        int sampleRate = 2048;
        double[] samples = new double[2048];
        for (int index = 0; index < samples.length; index++) {
            samples[index] = Math.sin(2 * Math.PI * 64 * index / sampleRate);
        }

        SpectrumFrame spectrum = new SpectrumAnalyzer()
                .analyze(new AudioSamples(samples, sampleRate))
                .frames()
                .getFirst();

        int strongestBin = 0;
        for (int index = 1; index < spectrum.magnitudes().length; index++) {
            if (spectrum.magnitudes()[index] > spectrum.magnitudes()[strongestBin]) {
                strongestBin = index;
            }
        }
        assertThat(strongestBin).isEqualTo(64);
    }

    @Test
    void fingerprintingIsDeterministicAndProducesOccurrences() {
        double[] samples = sineWave(TEST_SAMPLE_RATE, TEST_DURATION_SECONDS, 440);

        List<FingerprintOccurrence> first = fingerprinting.fingerprint(samples, TEST_SAMPLE_RATE);
        List<FingerprintOccurrence> second = fingerprinting.fingerprint(samples, TEST_SAMPLE_RATE);

        assertThat(first).isNotEmpty().isEqualTo(second);
        assertThat(first).allSatisfy(occurrence -> assertThat(occurrence.hash()).isBetween(0L, 0xFFFF_FFFFL));
    }

    @Test
    void rejectsUnsupportedInput() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> fingerprinting.fingerprint(new double[0], TEST_SAMPLE_RATE));
        assertThatIllegalArgumentException().isThrownBy(() -> fingerprinting.fingerprint(new double[] {1}, 44_101));
    }

    @Test
    void fanOutConfigurationChangesFingerprintCount() {
        double[] samples = multiTone(TEST_SAMPLE_RATE, TEST_DURATION_SECONDS);

        List<FingerprintOccurrence> dense = new SpectralFingerprinting(30, 20).fingerprint(samples, TEST_SAMPLE_RATE);
        List<FingerprintOccurrence> sparse = new SpectralFingerprinting(30, 5).fingerprint(samples, TEST_SAMPLE_RATE);

        assertThat(sparse).isNotEmpty().hasSizeLessThan(dense.size());
    }

    private static double[] sineWave(int sampleRate, int seconds, double frequency) {
        double[] samples = new double[sampleRate * seconds];
        for (int index = 0; index < samples.length; index++) {
            samples[index] = Math.sin(2 * Math.PI * frequency * index / sampleRate);
        }
        return samples;
    }

    private static double[] multiTone(int sampleRate, int seconds) {
        double[] samples = new double[sampleRate * seconds];
        for (int index = 0; index < samples.length; index++) {
            double sample = 0;
            for (int frequency = 350; frequency <= 3_850; frequency += 100) {
                sample += Math.sin(2 * Math.PI * frequency * index / sampleRate);
            }
            samples[index] = sample / 36;
        }
        return samples;
    }
}
