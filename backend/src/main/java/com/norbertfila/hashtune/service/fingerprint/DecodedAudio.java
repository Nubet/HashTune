package com.norbertfila.hashtune.service.fingerprint;

public record DecodedAudio(double[] samples, int sampleRate) {
    public double durationSeconds() {
        return samples.length / (double) sampleRate;
    }
}
