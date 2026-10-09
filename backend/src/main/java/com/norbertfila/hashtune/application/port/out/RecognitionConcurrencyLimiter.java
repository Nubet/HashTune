package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.entity.identity.ExternalIdentity;

public interface RecognitionConcurrencyLimiter {
    boolean tryAcquire(ExternalIdentity owner);

    void release(ExternalIdentity owner);
}
