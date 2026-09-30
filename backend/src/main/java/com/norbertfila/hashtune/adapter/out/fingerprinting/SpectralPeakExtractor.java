package com.norbertfila.hashtune.adapter.out.fingerprinting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SpectralPeakExtractor {
    private static final double MIN_FREQUENCY_HZ = 300;
    private static final double MAX_FREQUENCY_HZ = 4_000;
    private static final double MIN_AMPLITUDE_DB = -60;
    private static final int TIME_NEIGHBOURHOOD = 10;
    private static final int FREQUENCY_NEIGHBOURHOOD = 5;
    private final int maxPeaksPerSecond;

    SpectralPeakExtractor() {
        this(30);
    }

    SpectralPeakExtractor(int maxPeaksPerSecond) {
        if (maxPeaksPerSecond < 1) {
            throw new IllegalArgumentException("maxPeaksPerSecond must be positive");
        }
        this.maxPeaksPerSecond = maxPeaksPerSecond;
    }

    List<SpectralPeak> extract(SpectrumFrames spectrum) {
        if (spectrum.frames().isEmpty()) {
            return List.of();
        }

        int minimumBin = Math.max(
                0, frequencyBin(MIN_FREQUENCY_HZ, spectrum.frames().getFirst().sampleRate()));
        int maximumBin = Math.min(
                SpectrumAnalyzer.FFT_WINDOW_SIZE / 2,
                frequencyBin(MAX_FREQUENCY_HZ, spectrum.frames().getFirst().sampleRate()) + 1);
        double maximumMagnitude = spectrum.frames().stream()
                .flatMapToDouble(frame -> java.util.Arrays.stream(frame.magnitudes(), minimumBin, maximumBin))
                .max()
                .orElse(Double.NEGATIVE_INFINITY);
        double amplitudeThreshold = maximumMagnitude + MIN_AMPLITUDE_DB;

        List<PeakCandidate> candidates = new ArrayList<>();
        for (int timeIndex = 0; timeIndex < spectrum.frames().size(); timeIndex++) {
            double[] magnitudes = spectrum.frames().get(timeIndex).magnitudes();
            for (int frequencyBin = minimumBin; frequencyBin < maximumBin; frequencyBin++) {
                double magnitude = magnitudes[frequencyBin];
                if (magnitude >= amplitudeThreshold && isLocalMaximum(spectrum, timeIndex, frequencyBin, magnitude)) {
                    candidates.add(new PeakCandidate(timeIndex, frequencyBin, magnitude));
                }
            }
        }

        double durationSeconds = spectrum.frames().getLast().timeSeconds()
                + SpectrumAnalyzer.FRAME_HOP_SIZE
                        / (double) spectrum.frames().getFirst().sampleRate();
        int maximumPeaks = Math.max(1, (int) Math.floor(durationSeconds * maxPeaksPerSecond));
        if (candidates.size() > maximumPeaks) {
            candidates.sort(Comparator.comparingDouble(PeakCandidate::magnitude).reversed());
            candidates = new ArrayList<>(candidates.subList(0, maximumPeaks));
        }

        return candidates.stream()
                .sorted(Comparator.comparingInt(PeakCandidate::timeIndex).thenComparingInt(PeakCandidate::frequencyBin))
                .map(candidate -> {
                    SpectrumFrame frame = spectrum.frames().get(candidate.timeIndex());
                    return new SpectralPeak(
                            candidate.frequencyBin(),
                            frame.frequencyHzForBin(candidate.frequencyBin()),
                            frame.timeSeconds());
                })
                .toList();
    }

    private boolean isLocalMaximum(SpectrumFrames spectrum, int timeIndex, int frequencyBin, double magnitude) {
        int firstTime = Math.max(0, timeIndex - TIME_NEIGHBOURHOOD);
        int lastTime = Math.min(spectrum.frames().size(), timeIndex + TIME_NEIGHBOURHOOD + 1);
        int firstFrequency = Math.max(0, frequencyBin - FREQUENCY_NEIGHBOURHOOD);
        int lastFrequency = Math.min(SpectrumAnalyzer.FFT_WINDOW_SIZE / 2, frequencyBin + FREQUENCY_NEIGHBOURHOOD + 1);

        for (int neighbourTime = firstTime; neighbourTime < lastTime; neighbourTime++) {
            double[] magnitudes = spectrum.frames().get(neighbourTime).magnitudes();
            for (int neighbourFrequency = firstFrequency; neighbourFrequency < lastFrequency; neighbourFrequency++) {
                if (neighbourTime == timeIndex && neighbourFrequency == frequencyBin) {
                    continue;
                }
                if (magnitudes[neighbourFrequency] >= magnitude) {
                    return false;
                }
            }
        }
        return true;
    }

    private int frequencyBin(double frequencyHz, int sampleRate) {
        return (int) Math.floor(frequencyHz * SpectrumAnalyzer.FFT_WINDOW_SIZE / sampleRate);
    }

    private record PeakCandidate(int timeIndex, int frequencyBin, double magnitude) {}
}
