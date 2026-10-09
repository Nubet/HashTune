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
    private long maxDurationMs = 1_800_000;
    private long probeTimeoutMs = 10_000;
    private long ffmpegTimeoutMs = 1_800_000;
    private long maxDecodedPcmBytes = 64L * 1024 * 1024;
    private long maxCoverArtBytes = 5L * 1024 * 1024;
    private String ffprobeBinary = "ffprobe";
    private Set<String> allowedContentTypes = new LinkedHashSet<>(Set.of(
            "audio/flac",
            "audio/mpeg",
            "audio/mp4",
            "audio/ogg",
            "audio/wav",
            "audio/webm",
            "audio/x-m4a",
            "audio/x-wav"));
    private Set<String> allowedCoverArtContentTypes = new LinkedHashSet<>(Set.of(
            "image/jpeg", "image/png", "image/webp"));
    private Set<String> allowedContainerNames = new LinkedHashSet<>(Set.of(
            "flac", "matroska", "webm", "mp3", "mov", "mp4", "m4a", "3gp", "3g2", "mj2", "ogg", "wav"));
}
