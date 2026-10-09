package com.norbertfila.hashtune.service.validation;

import com.norbertfila.hashtune.exceptions.application.ApplicationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AudioUploadValidatorTest {
    private final AudioSafetyProperties properties = new AudioSafetyProperties();
    private final AudioUploadValidator validator = new AudioUploadValidator(properties);

    @Test
    void rejectsEmptyAudio() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.mp3", "audio/mpeg", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Audio file is empty");
    }

    @Test
    void rejectsAudioAboveConfiguredSize() {
        properties.setMaxFileSizeBytes(2);
        MockMultipartFile file = new MockMultipartFile("file", "large.mp3", "audio/mpeg", new byte[3]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Audio file exceeds the maximum allowed size");
    }

    @Test
    void rejectsUnsupportedDeclaredContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", new byte[] {1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("The declared audio content type is not supported");
    }
}
