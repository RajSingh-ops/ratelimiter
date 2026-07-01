package com.example.ratelimiter.algorithm;

import com.example.ratelimiter.model.RateLimitAlgorithm;
import com.example.ratelimiter.model.RateLimitResult;

public interface RateLimiter {

    RateLimitResult check(String key);

    RateLimitAlgorithm getAlgorithm();
}
