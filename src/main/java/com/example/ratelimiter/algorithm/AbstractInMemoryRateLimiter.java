package com.example.ratelimiter.algorithm;

import com.example.ratelimiter.model.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractInMemoryRateLimiter implements RateLimiter, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AbstractInMemoryRateLimiter.class);

    private final ConcurrentHashMap<String, Long> lastAccessNanos = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupScheduler;
    private final long staleThresholdNanos;

    protected AbstractInMemoryRateLimiter(Duration cleanupInterval) {
        this.staleThresholdNanos = cleanupInterval.toNanos();

        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("ratelimiter-cleanup-" + getAlgorithm().name().toLowerCase());
            t.setDaemon(true);
            return t;
        });

        this.cleanupScheduler.scheduleAtFixedRate(
                this::performCleanup,
                cleanupInterval.toMillis(),
                cleanupInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );

        log.info("Initialized {} rate limiter with cleanup interval {}",
                getAlgorithm().getDisplayName(), cleanupInterval);
    }

    @Override
    public final RateLimitResult check(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Rate limit key must not be null or blank");
        }
        lastAccessNanos.put(key, System.nanoTime());
        return doCheck(key);
    }

    protected abstract RateLimitResult doCheck(String key);

    protected abstract void evictKey(String key);

    public void reset(String key) {
        evictKey(key);
        lastAccessNanos.remove(key);
        log.debug("Reset rate limit state for key: {}", key);
    }

    public int getTrackedKeyCount() {
        return lastAccessNanos.size();
    }

    private void performCleanup() {
        long now = System.nanoTime();
        int evicted = 0;

        for (var entry : lastAccessNanos.entrySet()) {
            if (now - entry.getValue() > staleThresholdNanos) {
                evictKey(entry.getKey());
                lastAccessNanos.remove(entry.getKey());
                evicted++;
            }
        }

        if (evicted > 0) {
            log.debug("Cleanup evicted {} stale keys from {} limiter",
                    evicted, getAlgorithm().getDisplayName());
        }
    }

    @Override
    public void destroy() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
                log.warn("Cleanup scheduler for {} did not terminate gracefully",
                        getAlgorithm().getDisplayName());
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
