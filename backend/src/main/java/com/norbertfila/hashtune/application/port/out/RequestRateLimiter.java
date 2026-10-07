package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.identity.ExternalIdentity;

public interface RequestRateLimiter {
    RateLimitDecision check(ExternalIdentity identity, String clientIp, boolean expensiveRequest);
}
