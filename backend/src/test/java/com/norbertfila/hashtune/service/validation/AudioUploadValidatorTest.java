package com.norbertfila.hashtune.service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import com.norbertfila.hashtune.exceptions.application.ApplicationException;
import com.norbertfila.hashtune.service.audio.AudioInput;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class AudioUploadValidatorTest {
    private final AudioSafetyProperties properties = new AudioSafetyProperties();
    private final AudioUploadValidator validator = new AudioUploadValidator(properties);

    @Test
    void rejectsEmptyAudio() {
        AudioInput file = audio("empty.mp3", "audio/mpeg", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Audio file is empty");
    }

    @Test
    void rejectsAudioAboveConfiguredSize() {
        properties.setMaxFileSizeBytes(2);
        AudioInput file = audio("large.mp3", "audio/mpeg", new byte[3]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Audio file exceeds the maximum allowed size");
    }

    @Test
    void rejectsUnsupportedDeclaredContentType() {
        AudioInput file = audio("sample.txt", "text/plain", new byte[] {1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("The declared audio content type is not supported");
    }

    private AudioInput audio(String name, String contentType, byte[] data) {
        return new AudioInput(name, contentType, data.length, () -> new ByteArrayInputStream(data));
    }
}
