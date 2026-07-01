package com.example.ratelimiter.model;

public enum RateLimitAlgorithm {

    FIXED_WINDOW("Fixed Window Counter"),
    SLIDING_WINDOW_LOG("Sliding Window Log"),
    SLIDING_WINDOW_COUNTER("Sliding Window Counter"),
    TOKEN_BUCKET("Token Bucket"),
    LEAKY_BUCKET("Leaky Bucket");

    private final String displayName;

    RateLimitAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
