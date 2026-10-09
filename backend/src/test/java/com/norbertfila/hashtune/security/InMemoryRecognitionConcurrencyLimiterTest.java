package com.norbertfila.hashtune.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.norbertfila.hashtune.configuration.RateLimitProperties;
import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import org.junit.jupiter.api.Test;

class InMemoryRecognitionConcurrencyLimiterTest {
    @Test
    void allowsOnlyOneActiveRecognitionPerIdentity() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxConcurrentRecognitionsPerIdentity(1);
        InMemoryRecognitionConcurrencyLimiter limiter = new InMemoryRecognitionConcurrencyLimiter(properties);
        ExternalIdentity owner = new ExternalIdentity("test-issuer", "subject-a");

        assertThat(limiter.tryAcquire(owner)).isTrue();
        assertThat(limiter.tryAcquire(owner)).isFalse();

        limiter.release(owner);

        assertThat(limiter.tryAcquire(owner)).isTrue();
    }
}
