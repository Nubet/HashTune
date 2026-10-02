package com.norbertfila.hashtune.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.audio")
public class AudioProperties {
    private String engine;
    private String ffmpegBinary;
    private long maxFileSizeBytes;
    private FingerprintProperties fingerprint = new FingerprintProperties();

    @Getter
    @Setter
    public static class FingerprintProperties {
        private int maxPeaksPerSecond = 30;
        private int fanOut = 20;
    }
}
