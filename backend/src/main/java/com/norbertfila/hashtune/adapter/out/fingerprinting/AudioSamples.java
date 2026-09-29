package com.norbertfila.hashtune.adapter.out.fingerprinting;

public record AudioSamples(double[] values, int sampleRate) {
    public AudioSamples {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Audio samples must not be empty");
        }
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive");
        }
    }

    public double durationSeconds() {
        return values.length / (double) sampleRate;
    }
}
