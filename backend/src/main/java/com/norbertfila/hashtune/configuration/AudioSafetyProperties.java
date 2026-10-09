package com.norbertfila.hashtune.configuration;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.audio.safety")
public class AudioSafetyProperties {
    private long maxFileSizeBytes = 200L * 1024 * 1024;
    private Set<String> allowedContentTypes = new LinkedHashSet<>(Set.of(
            "audio/flac",
            "audio/mpeg",
            "audio/mp4",
            "audio/ogg",
            "audio/wav",
            "audio/webm",
            "audio/x-m4a",
            "audio/x-wav"));
}
