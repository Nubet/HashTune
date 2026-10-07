package com.norbertfila.hashtune.adapter.out.ratelimit;

import com.norbertfila.hashtune.application.port.out.RateLimitDecision;
import com.norbertfila.hashtune.application.port.out.RequestRateLimiter;
import com.norbertfila.hashtune.configuration.RateLimitProperties;
import com.norbertfila.hashtune.domain.session.ClientSessionId;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Bucket4jRequestRateLimiter implements RequestRateLimiter {
    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;

    public Bucket4jRequestRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public RateLimitDecision check(ClientSessionId sessionId, String clientIp) {
        RateLimitDecision sessionDecision =
                consume("session:" + sessionId.value(), properties.getSessionCapacity(), properties.getSessionRefill());
        if (!sessionDecision.allowed()) {
            return sessionDecision;
        }
        return consume("ip:" + clientIp, properties.getIpCapacity(), properties.getIpRefill());
    }

    @Scheduled(fixedDelayString = "${app.rate-limit.bucket-cleanup-delay:10m}")
    void removeExpiredBuckets() {
        long cutoff =
                System.currentTimeMillis() - properties.getBucketRetention().toMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccess() < cutoff);
    }

    private RateLimitDecision consume(String key, int capacity, Duration refillPeriod) {
        BucketEntry entry =
                buckets.computeIfAbsent(key, ignored -> new BucketEntry(createBucket(capacity, refillPeriod)));
        entry.touch();
        var probe = entry.bucket().tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitDecision.permit();
        }
        return RateLimitDecision.reject(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    private Bucket createBucket(int capacity, Duration refillPeriod) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod)
                        .build())
                .build();
    }

    private static final class BucketEntry {
        private final Bucket bucket;
        private volatile long lastAccess = System.currentTimeMillis();

        private BucketEntry(Bucket bucket) {
            this.bucket = bucket;
        }

        private Bucket bucket() {
            return bucket;
        }

        private long lastAccess() {
            return lastAccess;
        }

        private void touch() {
            lastAccess = System.currentTimeMillis();
        }
    }
}
