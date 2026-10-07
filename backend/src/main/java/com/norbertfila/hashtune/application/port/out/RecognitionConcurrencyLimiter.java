package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.session.ClientSessionId;

public interface RecognitionConcurrencyLimiter {
    boolean tryAcquire(ClientSessionId sessionId);

    void release(ClientSessionId sessionId);
}
