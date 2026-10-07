package com.norbertfila.hashtune.adapter.out.ratelimit;

import com.norbertfila.hashtune.application.port.out.RecognitionConcurrencyLimiter;
import com.norbertfila.hashtune.configuration.RateLimitProperties;
import com.norbertfila.hashtune.domain.session.ClientSessionId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRecognitionConcurrencyLimiter implements RecognitionConcurrencyLimiter {
    private final Map<String, Semaphore> permits = new ConcurrentHashMap<>();
    private final int permitsPerSession;

    public InMemoryRecognitionConcurrencyLimiter(RateLimitProperties properties) {
        permitsPerSession = properties.getMaxConcurrentRecognitionsPerSession();
    }

    @Override
    public boolean tryAcquire(ClientSessionId sessionId) {
        return permits.computeIfAbsent(sessionId.value(), ignored -> new Semaphore(permitsPerSession))
                .tryAcquire();
    }

    @Override
    public void release(ClientSessionId sessionId) {
        Semaphore semaphore = permits.get(sessionId.value());
        if (semaphore != null) {
            semaphore.release();
        }
    }

    @Scheduled(fixedDelayString = "${app.rate-limit.bucket-cleanup-delay:10m}")
    void removeIdlePermits() {
        permits.entrySet().removeIf(entry -> entry.getValue().availablePermits() == permitsPerSession);
    }
}
