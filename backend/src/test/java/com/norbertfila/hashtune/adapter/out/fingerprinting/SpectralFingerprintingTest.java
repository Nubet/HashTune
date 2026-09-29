package com.norbertfila.hashtune.adapter.out.fingerprinting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpectralFingerprintingTest {
    private final SpectralFingerprinting fingerprinting = new SpectralFingerprinting();

    @Test
    void downsampleAveragesEveryInputGroup() {
        double[] result = AudioPreprocessor.downsample(new double[] {1, 3, 5, 7, 9}, 2);

        assertThat(result).containsExactly(2, 6, 9);
    }

    @Test
    void spectrumAnalyzerFindsTheExpectedFrequencyBin() {
        int sampleRate = 1024;
        double[] samples = new double[1024];
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
        double[] samples = sineWave(44_100, 2, 440);

        List<FingerprintOccurrence> first = fingerprinting.fingerprint(samples, 44_100);
        List<FingerprintOccurrence> second = fingerprinting.fingerprint(samples, 44_100);

        assertThat(first).isNotEmpty().isEqualTo(second);
        assertThat(first).allSatisfy(occurrence -> assertThat(occurrence.hash()).isBetween(0L, 0xFFFF_FFFFL));
    }

    @Test
    void rejectsUnsupportedInput() {
        assertThatIllegalArgumentException().isThrownBy(() -> fingerprinting.fingerprint(new double[0], 44_100));
        assertThatIllegalArgumentException().isThrownBy(() -> fingerprinting.fingerprint(new double[] {1}, 44_101));
    }

    private static double[] sineWave(int sampleRate, int seconds, double frequency) {
        double[] samples = new double[sampleRate * seconds];
        for (int index = 0; index < samples.length; index++) {
            samples[index] = Math.sin(2 * Math.PI * frequency * index / sampleRate);
        }
        return samples;
    }
}
