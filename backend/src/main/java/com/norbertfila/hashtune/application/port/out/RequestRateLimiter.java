package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.session.ClientSessionId;

public interface RequestRateLimiter {
    RateLimitDecision check(ClientSessionId sessionId, String clientIp);
}
