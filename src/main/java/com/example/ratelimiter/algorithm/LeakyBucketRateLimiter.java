package com.example.ratelimiter.algorithm;

import com.example.ratelimiter.model.RateLimitAlgorithm;
import com.example.ratelimiter.model.RateLimitResult;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class LeakyBucketRateLimiter extends AbstractInMemoryRateLimiter {

    private final int bucketCapacity;
    private final double leakRatePerNano;

    private final ConcurrentHashMap<String, AtomicReference<LeakyBucketState>> buckets =
            new ConcurrentHashMap<>();

    private record LeakyBucketState(double waterLevel, long lastLeakNanos) {
    }

    public LeakyBucketRateLimiter(int bucketCapacity, double leakRatePerSecond,
                                  Duration cleanupInterval) {
        super(cleanupInterval);
        this.bucketCapacity = bucketCapacity;
        this.leakRatePerNano = leakRatePerSecond / 1_000_000_000.0;
    }

    @Override
    protected RateLimitResult doCheck(String key) {
        long now = System.nanoTime();

        AtomicReference<LeakyBucketState> ref = buckets.computeIfAbsent(key,
                k -> new AtomicReference<>(new LeakyBucketState(0.0, now)));

        while (true) {
            LeakyBucketState current = ref.get();

            double elapsed = now - current.lastLeakNanos();
            double leaked = elapsed * leakRatePerNano;
            double currentWater = Math.max(0, current.waterLevel() - leaked);

            if (currentWater + 1.0 <= bucketCapacity) {
                LeakyBucketState next = new LeakyBucketState(currentWater + 1.0, now);
                if (ref.compareAndSet(current, next)) {
                    long remaining = (long) (bucketCapacity - next.waterLevel());
                    return RateLimitResult.allowed(remaining, bucketCapacity, getAlgorithm());
                }
                continue;
            }

            double overflow = currentWater + 1.0 - bucketCapacity;
            long waitNanos = (long) (overflow / leakRatePerNano);

            return RateLimitResult.rejected(
                    Duration.ofNanos(Math.max(1, waitNanos)),
                    bucketCapacity,
                    getAlgorithm());
        }
    }

    @Override
    public RateLimitAlgorithm getAlgorithm() {
        return RateLimitAlgorithm.LEAKY_BUCKET;
    }

    @Override
    protected void evictKey(String key) {
        buckets.remove(key);
    }
}
