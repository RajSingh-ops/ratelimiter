package com.example.ratelimiter.algorithm;

import com.example.ratelimiter.model.RateLimitAlgorithm;
import com.example.ratelimiter.model.RateLimitResult;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class SlidingWindowCounterRateLimiter extends AbstractInMemoryRateLimiter {

    private final long maxRequests;
    private final long windowSizeNanos;

    private final ConcurrentHashMap<String, AtomicReference<SlidingWindowState>> windows =
            new ConcurrentHashMap<>();

    private record SlidingWindowState(
            long currentWindowStartNanos,
            long currentCount,
            long previousCount
    ) {
    }

    public SlidingWindowCounterRateLimiter(long maxRequests, Duration windowSize, Duration cleanupInterval) {
        super(cleanupInterval);
        this.maxRequests = maxRequests;
        this.windowSizeNanos = windowSize.toNanos();
    }

    @Override
    protected RateLimitResult doCheck(String key) {
        long now = System.nanoTime();

        AtomicReference<SlidingWindowState> ref = windows.computeIfAbsent(key,
                k -> new AtomicReference<>(new SlidingWindowState(now, 0, 0)));

        while (true) {
            SlidingWindowState current = ref.get();

            long effectiveWindowStart = current.currentWindowStartNanos();
            long effectivePrevCount = current.previousCount();
            long effectiveCurrCount = current.currentCount();
            long elapsed = now - effectiveWindowStart;

            if (elapsed >= 2 * windowSizeNanos) {
                effectiveWindowStart = now;
                effectivePrevCount = 0;
                effectiveCurrCount = 0;
                elapsed = 0;
            } else if (elapsed >= windowSizeNanos) {
                effectivePrevCount = effectiveCurrCount;
                effectiveCurrCount = 0;
                effectiveWindowStart = effectiveWindowStart + windowSizeNanos;
                elapsed = now - effectiveWindowStart;
            }

            double elapsedFraction = (double) elapsed / windowSizeNanos;
            double weightedCount = effectivePrevCount * (1.0 - elapsedFraction) + effectiveCurrCount;

            if (weightedCount >= maxRequests) {
                long retryNanos = windowSizeNanos - elapsed;
                return RateLimitResult.rejected(
                        Duration.ofNanos(Math.max(1, retryNanos)),
                        maxRequests,
                        getAlgorithm());
            }

            SlidingWindowState next = new SlidingWindowState(
                    effectiveWindowStart,
                    effectiveCurrCount + 1,
                    effectivePrevCount
            );

            if (ref.compareAndSet(current, next)) {
                double newWeightedCount = effectivePrevCount * (1.0 - elapsedFraction) + effectiveCurrCount + 1;
                long remaining = Math.max(0, (long) (maxRequests - newWeightedCount));
                return RateLimitResult.allowed(remaining, maxRequests, getAlgorithm());
            }
        }
    }

    @Override
    public RateLimitAlgorithm getAlgorithm() {
        return RateLimitAlgorithm.SLIDING_WINDOW_COUNTER;
    }

    @Override
    protected void evictKey(String key) {
        windows.remove(key);
    }
}
