package com.norbertfila.hashtune.adapter.out.fingerprinting;

import java.util.ArrayList;
import java.util.List;
import org.jtransforms.fft.DoubleFFT_1D;

final class SpectrumAnalyzer {
    static final int FFT_WINDOW_SIZE = 2048;
    static final int FRAME_HOP_SIZE = 512;
    private final DoubleFFT_1D fft = new DoubleFFT_1D(FFT_WINDOW_SIZE);

    SpectrumFrames analyze(AudioSamples audio) {
        double[] window = createHanningWindow();
        List<SpectrumFrame> frames = new ArrayList<>();

        for (int frameStart = 0; frameStart + FFT_WINDOW_SIZE <= audio.values().length; frameStart += FRAME_HOP_SIZE) {
            frames.add(analyzeFrame(audio.values(), frameStart, audio.sampleRate(), window));
        }
        return new SpectrumFrames(frames);
    }

    private SpectrumFrame analyzeFrame(double[] samples, int frameStart, int sampleRate, double[] window) {
        double[] windowedSamples = new double[FFT_WINDOW_SIZE];
        for (int index = 0; index < FFT_WINDOW_SIZE; index++) {
            windowedSamples[index] = samples[frameStart + index] * window[index];
        }

        // JTransforms stores DC at index 0 and the remaining real FFT bins as real/imaginary pairs
        fft.realForward(windowedSamples);
        double[] magnitudes = new double[FFT_WINDOW_SIZE / 2];
        magnitudes[0] = decibels(Math.abs(windowedSamples[0]));
        for (int frequencyBin = 1; frequencyBin < magnitudes.length; frequencyBin++) {
            magnitudes[frequencyBin] =
                    decibels(Math.hypot(windowedSamples[2 * frequencyBin], windowedSamples[2 * frequencyBin + 1]));
        }
        return new SpectrumFrame(magnitudes, sampleRate, frameStart);
    }

    private double decibels(double magnitude) {
        return 20 * Math.log10(Math.max(magnitude, 1e-12));
    }

    private double[] createHanningWindow() {
        double[] window = new double[FFT_WINDOW_SIZE];
        for (int index = 0; index < window.length; index++) {
            window[index] = 0.5 - 0.5 * Math.cos(2 * Math.PI * index / window.length);
        }
        return window;
    }
}
