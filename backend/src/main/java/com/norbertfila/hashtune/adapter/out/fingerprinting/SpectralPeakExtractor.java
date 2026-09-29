package com.norbertfila.hashtune.adapter.out.fingerprinting;

import java.util.ArrayList;
import java.util.List;

final class SpectralPeakExtractor {
    // These are FFT-bin ranges, not Hz. The ranges widen at higher frequencies to limit peak density
    // The final boundary is half the FFT size because a real-valued signal has a mirrored spectrum
    private static final List<FrequencyBand> FREQUENCY_BANDS = List.of(
            new FrequencyBand(0, 10),
            new FrequencyBand(10, 20),
            new FrequencyBand(20, 40),
            new FrequencyBand(40, 80),
            new FrequencyBand(80, 160),
            new FrequencyBand(160, SpectrumAnalyzer.FFT_WINDOW_SIZE / 2));

    List<SpectralPeak> extract(SpectrumFrames spectrum) {
        if (spectrum.frames().isEmpty()) {
            return List.of();
        }

        List<SpectralPeak> peaks = new ArrayList<>();
        for (SpectrumFrame frame : spectrum.frames()) {
            peaks.addAll(findSignificantPeaks(frame));
        }
        return peaks;
    }

    private List<SpectralPeak> findSignificantPeaks(SpectrumFrame frame) {
        List<BandMaximum> bandMaximums = new ArrayList<>();
        for (FrequencyBand band : FREQUENCY_BANDS) {
            bandMaximums.add(findBandMaximum(frame, band));
        }

        double averageBandMagnitude = bandMaximums.stream()
                .mapToDouble(BandMaximum::magnitude)
                .average()
                .orElse(0);

        return bandMaximums.stream()
                .filter(maximum -> maximum.magnitude() > averageBandMagnitude)
                .map(maximum -> new SpectralPeak(frame.frequencyHzForBin(maximum.frequencyBin()), frame.timeSeconds()))
                .toList();
    }

    private BandMaximum findBandMaximum(SpectrumFrame frame, FrequencyBand band) {
        int lastBin = Math.min(band.lastBinExclusive(), frame.magnitudes().length);
        int strongestBin = band.firstBinInclusive();
        double strongestMagnitude = 0;

        for (int frequencyBin = band.firstBinInclusive(); frequencyBin < lastBin; frequencyBin++) {
            if (frame.magnitudes()[frequencyBin] > strongestMagnitude) {
                strongestMagnitude = frame.magnitudes()[frequencyBin];
                strongestBin = frequencyBin;
            }
        }
        return new BandMaximum(strongestBin, strongestMagnitude);
    }

    private record BandMaximum(int frequencyBin, double magnitude) {}
}
