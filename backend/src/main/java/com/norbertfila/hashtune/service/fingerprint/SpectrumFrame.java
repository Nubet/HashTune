package com.norbertfila.hashtune.service.fingerprint;

record SpectrumFrame(double[] magnitudes, int sampleRate, int startSample) {
    double frequencyHzForBin(int frequencyBin) {
        return frequencyBin * (sampleRate / (double) SpectrumAnalyzer.FFT_WINDOW_SIZE);
    }

    double timeSeconds() {
        return startSample / (double) sampleRate;
    }
}
