package com.norbertfila.hashtune.service.fingerprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.norbertfila.hashtune.exceptions.audio.AudioInputRejectedException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BoundedProcessOutputTest {
    @Test
    void readsOutputWithinLimit() throws Exception {
        byte[] output = BoundedProcessOutput.read(new ByteArrayInputStream("pcm".getBytes(StandardCharsets.UTF_8)), 3);

        assertThat(output).isEqualTo("pcm".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsOutputAboveLimit() {
        assertThatThrownBy(() -> BoundedProcessOutput.read(new ByteArrayInputStream(new byte[4]), 3))
                .isInstanceOf(AudioInputRejectedException.class)
                .hasMessage("Decoded audio exceeds the memory safety limit");
    }
}
