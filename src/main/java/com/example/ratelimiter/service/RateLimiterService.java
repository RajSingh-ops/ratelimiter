package com.example.ratelimiter.service;

import com.example.ratelimiter.algorithm.*;
import com.example.ratelimiter.config.RateLimiterProperties;
import com.example.ratelimiter.model.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RateLimiter defaultLimiter;
    private final RateLimiterProperties properties;

    private final ConcurrentHashMap<String, RateLimiter> limiterCache = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimiter defaultLimiter, RateLimiterProperties properties) {
        this.defaultLimiter = defaultLimiter;
        this.properties = properties;
        log.info("RateLimiterService initialized with algorithm: {}",
                defaultLimiter.getAlgorithm().getDisplayName());
    }

    public RateLimitResult check(String key) {
        return defaultLimiter.check(key);
    }

    public RateLimitResult check(String key, int limit, int windowSeconds) {
        if (limit <= 0 || windowSeconds <= 0) {
            return check(key);
        }

        String cacheKey = properties.getAlgorithm().name() + ":" + limit + ":" + windowSeconds;
        RateLimiter limiter = limiterCache.computeIfAbsent(cacheKey, k -> {
            log.debug("Creating per-endpoint limiter: limit={}, window={}s", limit, windowSeconds);
            return createLimiter(limit, Duration.ofSeconds(windowSeconds));
        });

        return limiter.check(key);
    }

    public void reset(String key) {
        if (defaultLimiter instanceof AbstractInMemoryRateLimiter inMemory) {
            inMemory.reset(key);
        }
        limiterCache.values().forEach(limiter -> {
            if (limiter instanceof AbstractInMemoryRateLimiter inMemory) {
                inMemory.reset(key);
            }
        });
        log.info("Reset rate limit state for key: {}", key);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("algorithm", defaultLimiter.getAlgorithm().getDisplayName());
        stats.put("algorithmCode", defaultLimiter.getAlgorithm().name());
        stats.put("maxRequests", properties.getMaxRequests());
        stats.put("windowSize", properties.getWindowSize().toString());
        stats.put("bucketCapacity", properties.getBucketCapacity());
        stats.put("refillRate", properties.getRefillRate());
        stats.put("leakRate", properties.getLeakRate());
        stats.put("keyStrategy", properties.getKeyStrategy().name());
        stats.put("globalEnabled", properties.isGlobalEnabled());

        if (defaultLimiter instanceof AbstractInMemoryRateLimiter inMemory) {
            stats.put("trackedKeys", inMemory.getTrackedKeyCount());
        }

        stats.put("cachedLimiters", limiterCache.size());

        return stats;
    }

    private RateLimiter createLimiter(long maxRequests, Duration windowSize) {
        Duration cleanup = properties.getCleanupInterval();
        return switch (properties.getAlgorithm()) {
            case FIXED_WINDOW -> new FixedWindowRateLimiter(maxRequests, windowSize, cleanup);
            case SLIDING_WINDOW_LOG -> new SlidingWindowLogRateLimiter(maxRequests, windowSize, cleanup);
            case SLIDING_WINDOW_COUNTER -> new SlidingWindowCounterRateLimiter(maxRequests, windowSize, cleanup);
            case TOKEN_BUCKET -> {
                double rate = (double) maxRequests / windowSize.toSeconds();
                yield new TokenBucketRateLimiter((int) maxRequests, rate, cleanup);
            }
            case LEAKY_BUCKET -> {
                double rate = (double) maxRequests / windowSize.toSeconds();
                yield new LeakyBucketRateLimiter((int) maxRequests, rate, cleanup);
            }
        };
    }
}
