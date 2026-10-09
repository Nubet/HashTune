package com.norbertfila.hashtune.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.norbertfila.hashtune.configuration.RateLimitProperties;
import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Bucket4jRequestRateLimiterTest {
    private final RateLimitProperties properties = new RateLimitProperties();
    private Bucket4jRequestRateLimiter limiter;

    @BeforeEach
    void setUp() {
        properties.setIdentityCapacity(2);
        properties.setIpCapacity(2);
        properties.setExpensiveIdentityCapacity(2);
        properties.setExpensiveIpCapacity(2);
        properties.setIdentityRefill(Duration.ofHours(1));
        properties.setIpRefill(Duration.ofHours(1));
        limiter = new Bucket4jRequestRateLimiter(properties);
    }

    @Test
    void limitsRequestsPerIdentity() {
        ExternalIdentity identity = new ExternalIdentity("test-issuer", "subject-a");

        assertThat(limiter.check(Optional.of(identity), "127.0.0.1", false).allowed()).isTrue();
        assertThat(limiter.check(Optional.of(identity), "127.0.0.1", false).allowed()).isTrue();
        assertThat(limiter.check(Optional.of(identity), "127.0.0.1", false).allowed()).isFalse();
    }

    @Test
    void limitsRequestsPerIpAcrossIdentities() {
        assertThat(limiter.check(Optional.of(new ExternalIdentity("test-issuer", "subject-a")), "127.0.0.1", false).allowed())
                .isTrue();
        assertThat(limiter.check(Optional.of(new ExternalIdentity("test-issuer", "subject-b")), "127.0.0.1", false).allowed())
                .isTrue();
        assertThat(limiter.check(Optional.of(new ExternalIdentity("test-issuer", "subject-c")), "127.0.0.1", false).allowed())
                .isFalse();
    }

    @Test
    void keepsReadAndExpensiveRequestBucketsSeparate() {
        properties.setIdentityCapacity(1);
        properties.setIpCapacity(1);
        properties.setExpensiveIdentityCapacity(1);
        properties.setExpensiveIpCapacity(1);
        limiter = new Bucket4jRequestRateLimiter(properties);
        ExternalIdentity identity = new ExternalIdentity("test-issuer", "subject-a");

        assertThat(limiter.check(Optional.of(identity), "127.0.0.1", false).allowed()).isTrue();
        assertThat(limiter.check(Optional.of(identity), "127.0.0.1", true).allowed()).isTrue();
        assertThat(limiter.check(Optional.of(identity), "127.0.0.1", false).allowed()).isFalse();
        assertThat(limiter.check(Optional.of(identity), "127.0.0.1", true).allowed()).isFalse();
    }
}
