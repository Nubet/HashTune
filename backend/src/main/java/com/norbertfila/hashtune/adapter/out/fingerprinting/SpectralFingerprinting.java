package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.domain.fingerprint.FingerprintOccurrence;
import java.util.List;

public final class SpectralFingerprinting {
    private final AudioPreprocessor preprocessor = new AudioPreprocessor();
    private final SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer();
    private final SpectralPeakExtractor peakExtractor;
    private final FingerprintEncoder fingerprintEncoder;

    public SpectralFingerprinting() {
        this(30, 20);
    }

    public SpectralFingerprinting(int maxPeaksPerSecond, int fanOut) {
        this.peakExtractor = new SpectralPeakExtractor(maxPeaksPerSecond);
        this.fingerprintEncoder = new FingerprintEncoder(fanOut);
    }

    public List<FingerprintOccurrence> fingerprint(double[] samples, int sampleRate) {
        return fingerprint(new AudioSamples(samples, sampleRate));
    }

    public List<FingerprintOccurrence> fingerprint(AudioSamples audio) {
        AudioSamples preprocessedAudio = preprocessor.preprocess(audio);
        SpectrumFrames spectrum = spectrumAnalyzer.analyze(preprocessedAudio);
        List<SpectralPeak> peaks = peakExtractor.extract(spectrum);
        return fingerprintEncoder.encode(peaks);
    }
}
