package com.norbertfila.hashtune.adapter.out.fingerprinting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.norbertfila.hashtune.application.port.out.AudioInputRejectedException;
import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class FfmpegAudioDecoderTest {
    @Test
    void decodesValidAudio() {
        DecodedAudio decoded = decoder(5_000_000, 1_000_000).decode(new ByteArrayInputStream(wav(1_000)));

        assertThat(decoded.durationSeconds()).isBetween(0.9, 1.1);
    }

    @Test
    void rejectsAudioThatExceedsDurationLimit() {
        assertThatThrownBy(() -> decoder(100, 1_000_000).decode(new ByteArrayInputStream(wav(1_000))))
                .isInstanceOf(AudioInputRejectedException.class)
                .hasMessage("Audio exceeds the maximum allowed duration");
    }

    @Test
    void rejectsDecodedOutputThatExceedsMemoryLimit() {
        assertThatThrownBy(() -> decoder(5_000_000, 100).decode(new ByteArrayInputStream(wav(1_000))))
                .isInstanceOf(AudioInputRejectedException.class)
                .hasMessage("Decoded audio exceeds the memory safety limit");
    }

    @Test
    void stopsDecoderWhenProcessTimeoutExpires() {
        AudioSafetyProperties properties = properties(10_000, 1_000_000, 5_000);
        properties.setFfmpegTimeoutMs(1);

        assertThatThrownBy(() -> new FfmpegAudioDecoder("ffmpeg", "ffprobe", properties)
                        .decode(new ByteArrayInputStream(wav(1_000))))
                .isInstanceOf(AudioInputRejectedException.class)
                .hasMessage("Audio decoding timed out");
    }

    private FfmpegAudioDecoder decoder(long maxDurationMs, long maxPcmBytes) {
        return new FfmpegAudioDecoder("ffmpeg", "ffprobe", properties(5_000, maxPcmBytes, maxDurationMs));
    }

    private AudioSafetyProperties properties(long maxDurationMs, long maxPcmBytes) {
        return properties(10_000, maxPcmBytes, maxDurationMs);
    }

    private AudioSafetyProperties properties(long probeTimeoutMs, long maxPcmBytes, long maxDurationMs) {
        AudioSafetyProperties properties = new AudioSafetyProperties();
        properties.setProbeTimeoutMs(probeTimeoutMs);
        properties.setFfmpegTimeoutMs(probeTimeoutMs);
        properties.setMaxDecodedPcmBytes(maxPcmBytes);
        properties.setMaxDurationMs(maxDurationMs);
        return properties;
    }

    private byte[] wav(int durationMs) {
        int sampleRate = 22_050;
        int sampleCount = sampleRate * durationMs / 1_000;
        int dataSize = sampleCount * Short.BYTES;
        ByteBuffer wav = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes());
        wav.putInt(36 + dataSize);
        wav.put("WAVEfmt ".getBytes());
        wav.putInt(16);
        wav.putShort((short) 1);
        wav.putShort((short) 1);
        wav.putInt(sampleRate);
        wav.putInt(sampleRate * Short.BYTES);
        wav.putShort((short) Short.BYTES);
        wav.putShort((short) 16);
        wav.put("data".getBytes());
        wav.putInt(dataSize);
        return wav.array();
    }
}
