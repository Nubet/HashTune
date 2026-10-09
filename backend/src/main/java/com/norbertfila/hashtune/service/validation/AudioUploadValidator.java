package com.norbertfila.hashtune.service.validation;

import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import com.norbertfila.hashtune.exceptions.ErrorCode;
import com.norbertfila.hashtune.exceptions.application.InvalidAudioException;
import com.norbertfila.hashtune.service.audio.AudioInput;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AudioUploadValidator {
    private final AudioSafetyProperties properties;

    public void validate(AudioInput file) {
        if (file == null || file.size() == 0) {
            throw new InvalidAudioException(ErrorCode.AUDIO_FILE_EMPTY, "Audio file is empty");
        }
        if (file.size() > properties.getMaxFileSizeBytes()) {
            throw new InvalidAudioException(
                    ErrorCode.AUDIO_FILE_TOO_LARGE, "Audio file exceeds the maximum allowed size");
        }
        String contentType = file.contentType();
        if (contentType != null
                && !contentType.isBlank()
                && !properties.getAllowedContentTypes().contains(normalizeContentType(contentType))) {
            throw new InvalidAudioException(
                    ErrorCode.AUDIO_MIME_NOT_ALLOWED, "The declared audio content type is not supported");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }
}
