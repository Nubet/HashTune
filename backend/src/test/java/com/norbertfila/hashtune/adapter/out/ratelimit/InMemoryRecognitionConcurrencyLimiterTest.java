package com.norbertfila.hashtune.adapter.out.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.norbertfila.hashtune.configuration.RateLimitProperties;
import com.norbertfila.hashtune.domain.session.ClientSessionId;
import org.junit.jupiter.api.Test;

class InMemoryRecognitionConcurrencyLimiterTest {
    @Test
    void allowsOnlyOneActiveRecognitionPerSession() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxConcurrentRecognitionsPerSession(1);
        InMemoryRecognitionConcurrencyLimiter limiter = new InMemoryRecognitionConcurrencyLimiter(properties);
        ClientSessionId session = new ClientSessionId("session-a");

        assertThat(limiter.tryAcquire(session)).isTrue();
        assertThat(limiter.tryAcquire(session)).isFalse();

        limiter.release(session);

        assertThat(limiter.tryAcquire(session)).isTrue();
    }
}
