package com.norbertfila.hashtune.security;

import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import java.util.Optional;

public interface RequestRateLimiter {
    RateLimitDecision check(Optional<ExternalIdentity> identity, String clientIp, boolean expensiveRequest);
}
