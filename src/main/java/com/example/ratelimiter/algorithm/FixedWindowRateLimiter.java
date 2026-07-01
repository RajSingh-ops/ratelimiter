package com.example.ratelimiter.algorithm;

import com.example.ratelimiter.model.RateLimitAlgorithm;
import com.example.ratelimiter.model.RateLimitResult;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class FixedWindowRateLimiter extends AbstractInMemoryRateLimiter {

    private final long maxRequests;
    private final long windowSizeNanos;

    private final ConcurrentHashMap<String, AtomicReference<WindowSnapshot>> windows =
            new ConcurrentHashMap<>();

    private record WindowSnapshot(long windowStartNanos, long count) {
    }

    public FixedWindowRateLimiter(long maxRequests, Duration windowSize, Duration cleanupInterval) {
        super(cleanupInterval);
        this.maxRequests = maxRequests;
        this.windowSizeNanos = windowSize.toNanos();
    }

    @Override
    protected RateLimitResult doCheck(String key) {
        long now = System.nanoTime();

        AtomicReference<WindowSnapshot> ref = windows.computeIfAbsent(key,
                k -> new AtomicReference<>(new WindowSnapshot(now, 0)));

        while (true) {
            WindowSnapshot current = ref.get();

            if (now - current.windowStartNanos() >= windowSizeNanos) {
                WindowSnapshot next = new WindowSnapshot(now, 1);
                if (ref.compareAndSet(current, next)) {
                    return RateLimitResult.allowed(maxRequests - 1, maxRequests, getAlgorithm());
                }
                continue;
            }

            if (current.count() >= maxRequests) {
                long retryNanos = current.windowStartNanos() + windowSizeNanos - now;
                return RateLimitResult.rejected(
                        Duration.ofNanos(Math.max(0, retryNanos)),
                        maxRequests,
                        getAlgorithm());
            }

            WindowSnapshot next = new WindowSnapshot(current.windowStartNanos(), current.count() + 1);
            if (ref.compareAndSet(current, next)) {
                return RateLimitResult.allowed(
                        maxRequests - next.count(),
                        maxRequests,
                        getAlgorithm());
            }
        }
    }

    @Override
    public RateLimitAlgorithm getAlgorithm() {
        return RateLimitAlgorithm.FIXED_WINDOW;
    }

    @Override
    protected void evictKey(String key) {
        windows.remove(key);
    }
}
