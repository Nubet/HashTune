package com.norbertfila.hashtune.adapter.out.fingerprinting;

final class CanonicalAudioFormat {
    static final int SAMPLE_RATE = 22_050;
    static final int CHANNEL_COUNT = 1;
    static final int PCM16_NORMALIZATION_FACTOR = 1 << 15;
    static final String FFMPEG_OUTPUT_FORMAT = "s16le";
    static final String FFMPEG_AUDIO_CODEC = "pcm_s16le";
    static final String FFMPEG_OUTPUT_PIPE = "pipe:1";

    private CanonicalAudioFormat() {}
}
