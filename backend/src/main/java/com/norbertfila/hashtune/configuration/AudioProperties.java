package com.norbertfila.hashtune.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.audio")
public class AudioProperties {
    private String engine;
    private long maxFileSizeBytes;
}
