package com.norbertfila.hashtune.configuration;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private int identityCapacity = 10;
    private Duration identityRefill = Duration.ofMinutes(1);
    private int ipCapacity = 30;
    private Duration ipRefill = Duration.ofMinutes(1);
    private int expensiveIdentityCapacity = 10;
    private Duration expensiveIdentityRefill = Duration.ofMinutes(1);
    private int expensiveIpCapacity = 30;
    private Duration expensiveIpRefill = Duration.ofMinutes(1);
    private Duration bucketRetention = Duration.ofHours(1);
    private int maxConcurrentRecognitionsPerIdentity = 1;
}
