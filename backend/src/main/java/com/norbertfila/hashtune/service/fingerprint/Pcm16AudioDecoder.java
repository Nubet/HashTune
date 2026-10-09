package com.norbertfila.hashtune.service.fingerprint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class Pcm16AudioDecoder {
    private Pcm16AudioDecoder() {}

    static DecodedAudio decode(byte[] pcmBytes, int sampleRate) {
        if (pcmBytes.length % Short.BYTES != 0) {
            throw new IllegalArgumentException("PCM16 data must contain complete samples");
        }

        ByteBuffer pcm = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN);
        double[] samples = new double[pcmBytes.length / Short.BYTES];
        for (int index = 0; index < samples.length; index++) {
            samples[index] = pcm.getShort() / (double) CanonicalAudioFormat.PCM16_NORMALIZATION_FACTOR;
        }
        return new DecodedAudio(samples, sampleRate);
    }
}
