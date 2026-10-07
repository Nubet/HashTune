package com.norbertfila.hashtune.adapter.out.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.norbertfila.hashtune.configuration.RateLimitProperties;
import com.norbertfila.hashtune.domain.session.ClientSessionId;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Bucket4jRequestRateLimiterTest {
    private final RateLimitProperties properties = new RateLimitProperties();
    private Bucket4jRequestRateLimiter limiter;

    @BeforeEach
    void setUp() {
        properties.setSessionCapacity(2);
        properties.setIpCapacity(2);
        properties.setSessionRefill(Duration.ofHours(1));
        properties.setIpRefill(Duration.ofHours(1));
        limiter = new Bucket4jRequestRateLimiter(properties);
    }

    @Test
    void limitsRequestsPerSession() {
        ClientSessionId session = new ClientSessionId("session-a");

        assertThat(limiter.check(session, "127.0.0.1").allowed()).isTrue();
        assertThat(limiter.check(session, "127.0.0.1").allowed()).isTrue();
        assertThat(limiter.check(session, "127.0.0.1").allowed()).isFalse();
    }

    @Test
    void limitsRequestsPerIpAcrossSessions() {
        assertThat(limiter.check(new ClientSessionId("session-a"), "127.0.0.1").allowed())
                .isTrue();
        assertThat(limiter.check(new ClientSessionId("session-b"), "127.0.0.1").allowed())
                .isTrue();
        assertThat(limiter.check(new ClientSessionId("session-c"), "127.0.0.1").allowed())
                .isFalse();
    }
}
