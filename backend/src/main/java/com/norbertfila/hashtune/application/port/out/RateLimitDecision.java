package com.norbertfila.hashtune.application.port.out;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, Duration retryAfter) {
    public static RateLimitDecision permit() {
        return new RateLimitDecision(true, Duration.ZERO);
    }

    public static RateLimitDecision reject(Duration retryAfter) {
        return new RateLimitDecision(false, retryAfter);
    }
}
