package com.norbertfila.hashtune.service.validation;

import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import com.norbertfila.hashtune.exceptions.application.ApplicationException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class AudioUploadValidator {
    private final AudioSafetyProperties properties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "AUDIO_FILE_EMPTY",
                    "Audio file is empty");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE,
                    "AUDIO_FILE_TOO_LARGE",
                    "Audio file exceeds the maximum allowed size");
        }
        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.isBlank()
                && !properties.getAllowedContentTypes().contains(normalizeContentType(contentType))) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "AUDIO_MIME_NOT_ALLOWED",
                    "The declared audio content type is not supported");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }
}
