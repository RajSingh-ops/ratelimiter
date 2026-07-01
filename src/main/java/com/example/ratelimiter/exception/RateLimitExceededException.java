package com.example.ratelimiter.exception;

import com.example.ratelimiter.model.RateLimitResult;

import java.time.Duration;

public class RateLimitExceededException extends RuntimeException {

    private final Duration retryAfter;
    private final RateLimitResult result;

    public RateLimitExceededException(RateLimitResult result) {
        super("Rate limit exceeded. Retry after " + result.retryAfter().toMillis() + "ms");
        this.retryAfter = result.retryAfter();
        this.result = result;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    public RateLimitResult getResult() {
        return result;
    }
}
