package com.norbertfila.hashtune.security;

import com.norbertfila.hashtune.entity.identity.ExternalIdentity;

public interface RecognitionConcurrencyLimiter {
    boolean tryAcquire(ExternalIdentity owner);

    void release(ExternalIdentity owner);
}
