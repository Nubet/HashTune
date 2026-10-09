package com.norbertfila.hashtune.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.indexing")
public class IndexingProperties {
    private boolean workerEnabled = true;
    private int maxAttempts = 3;
    private long retryBackoffMs = 5_000;
    private long maxRetryBackoffMs = 300_000;
    private long staleProcessingTimeoutMs = 1_860_000;
}
