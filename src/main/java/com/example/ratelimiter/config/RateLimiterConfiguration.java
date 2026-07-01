package com.example.ratelimiter.config;

import com.example.ratelimiter.algorithm.*;
import com.example.ratelimiter.model.RateLimitAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfiguration.class);

    @Bean
    public RateLimiter rateLimiter(RateLimiterProperties props) {
        Duration cleanup = props.getCleanupInterval();

        RateLimiter limiter = switch (props.getAlgorithm()) {
            case FIXED_WINDOW -> new FixedWindowRateLimiter(
                    props.getMaxRequests(),
                    props.getWindowSize(),
                    cleanup);

            case SLIDING_WINDOW_LOG -> new SlidingWindowLogRateLimiter(
                    props.getMaxRequests(),
                    props.getWindowSize(),
                    cleanup);

            case SLIDING_WINDOW_COUNTER -> new SlidingWindowCounterRateLimiter(
                    props.getMaxRequests(),
                    props.getWindowSize(),
                    cleanup);

            case TOKEN_BUCKET -> new TokenBucketRateLimiter(
                    props.getBucketCapacity(),
                    props.getRefillRate(),
                    cleanup);

            case LEAKY_BUCKET -> new LeakyBucketRateLimiter(
                    props.getQueueCapacity(),
                    props.getLeakRate(),
                    cleanup);
        };

        log.info("Configured rate limiter: algorithm={}, maxRequests={}, windowSize={}, " +
                        "bucketCapacity={}, refillRate={}, leakRate={}",
                props.getAlgorithm().getDisplayName(),
                props.getMaxRequests(),
                props.getWindowSize(),
                props.getBucketCapacity(),
                props.getRefillRate(),
                props.getLeakRate());

        return limiter;
    }
}
