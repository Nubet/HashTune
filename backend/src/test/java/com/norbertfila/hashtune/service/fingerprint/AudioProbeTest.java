package com.norbertfila.hashtune.service.fingerprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.norbertfila.hashtune.exceptions.audio.AudioInputRejectedException;
import org.junit.jupiter.api.Test;

class AudioProbeTest {
    @Test
    void parsesAudioContainerAndDuration() {
        AudioProbe.Result result =
                AudioProbe.parseOutput("codec_type=audio\nformat_name=matroska,webm\nduration=12.5\n");

        assertThat(result.containerNames()).containsExactlyInAnyOrder("matroska", "webm");
        assertThat(result.durationMs()).isEqualTo(12_500);
    }

    @Test
    void rejectsOutputWithoutAudioOrDuration() {
        assertThatThrownBy(() -> AudioProbe.parseOutput("format_name=mp3\nduration=N/A\n"))
                .isInstanceOf(AudioInputRejectedException.class)
                .hasMessage("Audio must contain a supported audio stream");
    }
}
