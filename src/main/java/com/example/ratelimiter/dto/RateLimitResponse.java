package com.example.ratelimiter.dto;

import com.example.ratelimiter.model.RateLimitResult;

public record RateLimitResponse(
        boolean allowed,
        long remainingRequests,
        long retryAfterMs,
        long currentLimit,
        String algorithm,
        String timestamp
) {

    public static RateLimitResponse from(RateLimitResult result) {
        return new RateLimitResponse(
                result.allowed(),
                result.remainingRequests(),
                result.retryAfter().toMillis(),
                result.currentLimit(),
                result.algorithm().getDisplayName(),
                result.timestamp().toString()
        );
    }
}
