package com.norbertfila.hashtune.adapter.out.fingerprinting;

record SpectrumFrame(double[] magnitudes, int sampleRate, int startSample) {
    double frequencyHzForBin(int frequencyBin) {
        return frequencyBin * (sampleRate / (double) SpectrumAnalyzer.FFT_WINDOW_SIZE);
    }

    double timeSeconds() {
        return startSample / (double) sampleRate;
    }
}
