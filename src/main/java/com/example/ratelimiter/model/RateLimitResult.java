package com.example.ratelimiter.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record RateLimitResult(
        boolean allowed,
        long remainingRequests,
        Duration retryAfter,
        long currentLimit,
        RateLimitAlgorithm algorithm,
        Instant timestamp
) {

    public RateLimitResult {
        if (remainingRequests < 0) {
            throw new IllegalArgumentException(
                    "remainingRequests must be >= 0, got: " + remainingRequests);
        }
        Objects.requireNonNull(retryAfter, "retryAfter must not be null");
        if (currentLimit <= 0) {
            throw new IllegalArgumentException(
                    "currentLimit must be > 0, got: " + currentLimit);
        }
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static RateLimitResult allowed(long remainingRequests,
                                          long currentLimit,
                                          RateLimitAlgorithm algorithm) {
        return new RateLimitResult(
                true,
                remainingRequests,
                Duration.ZERO,
                currentLimit,
                algorithm,
                Instant.now()
        );
    }

    public static RateLimitResult rejected(Duration retryAfter,
                                           long currentLimit,
                                           RateLimitAlgorithm algorithm) {
        return new RateLimitResult(
                false,
                0,
                retryAfter,
                currentLimit,
                algorithm,
                Instant.now()
        );
    }
}
