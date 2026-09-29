package com.norbertfila.hashtune.adapter.out.fingerprinting;

final class AudioPreprocessor {
    static final int DOWN_SAMPLE_RATIO = 4;
    private static final double LOW_PASS_CUTOFF_HZ = 5_000;

    AudioSamples preprocess(AudioSamples audio) {
        if (audio.sampleRate() % DOWN_SAMPLE_RATIO != 0) {
            throw new IllegalArgumentException("Sample rate must be divisible by 4");
        }
        double[] filteredSamples = lowPassFilter(audio.values(), audio.sampleRate(), LOW_PASS_CUTOFF_HZ);
        double[] downsampledSamples = downsample(filteredSamples, DOWN_SAMPLE_RATIO);
        return new AudioSamples(downsampledSamples, audio.sampleRate() / DOWN_SAMPLE_RATIO);
    }

    private double[] lowPassFilter(double[] input, int sampleRate, double cutoffFrequencyHz) {
        double timeConstant = 1.0 / (2 * Math.PI * cutoffFrequencyHz);
        double timeStep = 1.0 / sampleRate;
        double smoothingFactor = timeStep / (timeConstant + timeStep);
        double[] filteredSamples = new double[input.length];
        double previousOutput = 0;

        for (int index = 0; index < input.length; index++) {
            double output = smoothingFactor * input[index] + (1 - smoothingFactor) * previousOutput;
            filteredSamples[index] = output;
            previousOutput = output;
        }
        return filteredSamples;
    }

    static double[] downsample(double[] input, int ratio) {
        double[] downsampledSamples = new double[(input.length + ratio - 1) / ratio];

        for (int outputIndex = 0; outputIndex < downsampledSamples.length; outputIndex++) {
            int firstInputIndex = outputIndex * ratio;
            int exclusiveEndIndex = Math.min(firstInputIndex + ratio, input.length);
            double groupSum = 0;
            for (int inputIndex = firstInputIndex; inputIndex < exclusiveEndIndex; inputIndex++) {
                groupSum += input[inputIndex];
            }
            downsampledSamples[outputIndex] = groupSum / (exclusiveEndIndex - firstInputIndex);
        }
        return downsampledSamples;
    }
}
