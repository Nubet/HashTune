package com.norbertfila.hashtune.adapter.out.fingerprinting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class Pcm16AudioDecoderTest {
    @Test
    void decodesLittleEndianPcm16ToNormalizedSamples() {
        byte[] pcm = ByteBuffer.allocate(6)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(Short.MIN_VALUE)
                .putShort((short) 0)
                .putShort(Short.MAX_VALUE)
                .array();

        DecodedAudio audio = Pcm16AudioDecoder.decode(pcm, CanonicalAudioFormat.SAMPLE_RATE);

        assertThat(audio.samples()).containsExactly(-1.0, 0.0, 0.999969482421875);
        assertThat(audio.sampleRate()).isEqualTo(CanonicalAudioFormat.SAMPLE_RATE);
    }

    @Test
    void rejectsIncompletePcm16Sample() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Pcm16AudioDecoder.decode(new byte[] {1}, CanonicalAudioFormat.SAMPLE_RATE));
    }
}
