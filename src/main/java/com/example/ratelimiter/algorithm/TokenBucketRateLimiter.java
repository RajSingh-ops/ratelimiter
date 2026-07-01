package com.example.ratelimiter.algorithm;

import com.example.ratelimiter.model.RateLimitAlgorithm;
import com.example.ratelimiter.model.RateLimitResult;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class TokenBucketRateLimiter extends AbstractInMemoryRateLimiter {

    private final int bucketCapacity;
    private final double refillRatePerNano;

    private final ConcurrentHashMap<String, AtomicReference<BucketState>> buckets =
            new ConcurrentHashMap<>();

    private record BucketState(double tokens, long lastRefillNanos) {
    }

    public TokenBucketRateLimiter(int bucketCapacity, double refillRatePerSecond,
                                  Duration cleanupInterval) {
        super(cleanupInterval);
        this.bucketCapacity = bucketCapacity;
        this.refillRatePerNano = refillRatePerSecond / 1_000_000_000.0;
    }

    @Override
    protected RateLimitResult doCheck(String key) {
        long now = System.nanoTime();

        AtomicReference<BucketState> ref = buckets.computeIfAbsent(key,
                k -> new AtomicReference<>(new BucketState(bucketCapacity, now)));

        while (true) {
            BucketState current = ref.get();

            double elapsed = now - current.lastRefillNanos();
            double refilled = Math.min(
                    bucketCapacity,
                    current.tokens() + elapsed * refillRatePerNano
            );

            if (refilled >= 1.0) {
                BucketState next = new BucketState(refilled - 1.0, now);
                if (ref.compareAndSet(current, next)) {
                    return RateLimitResult.allowed(
                            (long) next.tokens(),
                            bucketCapacity,
                            getAlgorithm());
                }
                continue;
            }

            double tokensNeeded = 1.0 - refilled;
            long waitNanos = (long) (tokensNeeded / refillRatePerNano);

            return RateLimitResult.rejected(
                    Duration.ofNanos(Math.max(1, waitNanos)),
                    bucketCapacity,
                    getAlgorithm());
        }
    }

    @Override
    public RateLimitAlgorithm getAlgorithm() {
        return RateLimitAlgorithm.TOKEN_BUCKET;
    }

    @Override
    protected void evictKey(String key) {
        buckets.remove(key);
    }
}
