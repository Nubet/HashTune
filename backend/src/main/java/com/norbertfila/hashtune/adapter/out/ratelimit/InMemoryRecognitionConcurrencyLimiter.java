package com.norbertfila.hashtune.adapter.out.ratelimit;

import com.norbertfila.hashtune.application.port.out.RecognitionConcurrencyLimiter;
import com.norbertfila.hashtune.configuration.RateLimitProperties;
import com.norbertfila.hashtune.domain.identity.ExternalIdentity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRecognitionConcurrencyLimiter implements RecognitionConcurrencyLimiter {
    private final Map<ExternalIdentity, Semaphore> permits = new ConcurrentHashMap<>();
    private final int permitsPerIdentity;

    public InMemoryRecognitionConcurrencyLimiter(RateLimitProperties properties) {
        permitsPerIdentity = properties.getMaxConcurrentRecognitionsPerIdentity();
    }

    @Override
    public boolean tryAcquire(ExternalIdentity owner) {
        return permits.computeIfAbsent(owner, ignored -> new Semaphore(permitsPerIdentity))
                .tryAcquire();
    }

    @Override
    public void release(ExternalIdentity owner) {
        Semaphore semaphore = permits.get(owner);
        if (semaphore != null) {
            semaphore.release();
        }
    }

    @Scheduled(fixedDelayString = "${app.rate-limit.bucket-cleanup-delay:10m}")
    void removeIdlePermits() {
        permits.entrySet().removeIf(entry -> entry.getValue().availablePermits() == permitsPerIdentity);
    }
}
