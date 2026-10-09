package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import java.util.Optional;

public interface RequestRateLimiter {
    RateLimitDecision check(Optional<ExternalIdentity> identity, String clientIp, boolean expensiveRequest);
}
