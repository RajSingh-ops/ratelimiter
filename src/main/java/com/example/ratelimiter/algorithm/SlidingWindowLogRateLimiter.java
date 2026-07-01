package com.example.ratelimiter.algorithm;

import com.example.ratelimiter.model.RateLimitAlgorithm;
import com.example.ratelimiter.model.RateLimitResult;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowLogRateLimiter extends AbstractInMemoryRateLimiter {

    private final long maxRequests;
    private final long windowSizeNanos;

    private final ConcurrentHashMap<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    public SlidingWindowLogRateLimiter(long maxRequests, Duration windowSize, Duration cleanupInterval) {
        super(cleanupInterval);
        this.maxRequests = maxRequests;
        this.windowSizeNanos = windowSize.toNanos();
    }

    @Override
    protected RateLimitResult doCheck(String key) {
        long now = System.nanoTime();
        long windowStart = now - windowSizeNanos;

        boolean[] allowed = {false};
        long[] remaining = {0};
        long[] retryAfterNanos = {0};

        requestLogs.compute(key, (k, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }

            while (!deque.isEmpty() && deque.peekFirst() <= windowStart) {
                deque.pollFirst();
            }

            if (deque.size() < maxRequests) {
                deque.addLast(now);
                allowed[0] = true;
                remaining[0] = maxRequests - deque.size();
            } else {
                allowed[0] = false;
                remaining[0] = 0;
                long oldest = deque.peekFirst();
                retryAfterNanos[0] = (oldest + windowSizeNanos) - now;
            }

            return deque;
        });

        if (allowed[0]) {
            return RateLimitResult.allowed(remaining[0], maxRequests, getAlgorithm());
        } else {
            return RateLimitResult.rejected(
                    Duration.ofNanos(Math.max(0, retryAfterNanos[0])),
                    maxRequests,
                    getAlgorithm());
        }
    }

    @Override
    public RateLimitAlgorithm getAlgorithm() {
        return RateLimitAlgorithm.SLIDING_WINDOW_LOG;
    }

    @Override
    protected void evictKey(String key) {
        requestLogs.remove(key);
    }
}
