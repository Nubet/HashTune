package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.identity.ExternalIdentity;

public interface RecognitionConcurrencyLimiter {
    boolean tryAcquire(ExternalIdentity owner);

    void release(ExternalIdentity owner);
}
