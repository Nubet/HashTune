package com.norbertfila.hashtune.adapter.out.fingerprinting;

final class AudioPreprocessor {
    AudioSamples preprocess(AudioSamples audio) {
        if (audio.sampleRate() != CanonicalAudioFormat.SAMPLE_RATE) {
            throw new IllegalArgumentException("Audio must use the canonical sample rate");
        }
        double[] centeredSamples = removeDcOffset(audio.values());
        return new AudioSamples(centeredSamples, audio.sampleRate());
    }

    private double[] removeDcOffset(double[] input) {
        double mean = 0;
        for (double sample : input) {
            mean += sample;
        }
        mean /= input.length;

        double[] centeredSamples = new double[input.length];
        for (int index = 0; index < input.length; index++) {
            centeredSamples[index] = input[index] - mean;
        }
        return centeredSamples;
    }
}
