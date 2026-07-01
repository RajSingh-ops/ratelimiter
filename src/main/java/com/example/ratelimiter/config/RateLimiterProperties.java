package com.example.ratelimiter.config;

import com.example.ratelimiter.model.RateLimitAlgorithm;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "rate-limiter")
@Validated
public class RateLimiterProperties {

    private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;

    @Positive
    private long maxRequests = 100;

    private Duration windowSize = Duration.ofMinutes(1);

    @Positive
    private int bucketCapacity = 100;

    @Positive
    private double refillRate = 10.0;

    @Positive
    private double leakRate = 10.0;

    @Positive
    private int queueCapacity = 100;

    private KeyStrategy keyStrategy = KeyStrategy.IP;

    private Duration cleanupInterval = Duration.ofMinutes(5);

    private boolean globalEnabled = true;

    public enum KeyStrategy {
        IP,
        USER,
        API_KEY,
        HEADER
    }

    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public long getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(long maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Duration getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(Duration windowSize) {
        this.windowSize = windowSize;
    }

    public int getBucketCapacity() {
        return bucketCapacity;
    }

    public void setBucketCapacity(int bucketCapacity) {
        this.bucketCapacity = bucketCapacity;
    }

    public double getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(double refillRate) {
        this.refillRate = refillRate;
    }

    public double getLeakRate() {
        return leakRate;
    }

    public void setLeakRate(double leakRate) {
        this.leakRate = leakRate;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public KeyStrategy getKeyStrategy() {
        return keyStrategy;
    }

    public void setKeyStrategy(KeyStrategy keyStrategy) {
        this.keyStrategy = keyStrategy;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    public boolean isGlobalEnabled() {
        return globalEnabled;
    }

    public void setGlobalEnabled(boolean globalEnabled) {
        this.globalEnabled = globalEnabled;
    }
}
