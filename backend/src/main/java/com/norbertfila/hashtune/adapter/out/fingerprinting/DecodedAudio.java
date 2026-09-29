package com.norbertfila.hashtune.adapter.out.fingerprinting;

public record DecodedAudio(double[] samples, int sampleRate) {
    public double durationSeconds() {
        return samples.length / (double) sampleRate;
    }
}
