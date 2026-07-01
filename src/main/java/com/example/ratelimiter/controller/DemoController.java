package com.example.ratelimiter.controller;

import com.example.ratelimiter.annotation.RateLimit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @RateLimit
    @GetMapping("/open")
    public ResponseEntity<Map<String, Object>> openEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "This endpoint uses the global rate limit config",
                "timestamp", Instant.now().toString()
        ));
    }

    @RateLimit(limit = 10, windowSeconds = 60)
    @GetMapping("/strict")
    public ResponseEntity<Map<String, Object>> strictEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "This endpoint allows only 10 requests per minute",
                "timestamp", Instant.now().toString()
        ));
    }

    @RateLimit(limit = 50, windowSeconds = 10, keyPrefix = "demo-burst")
    @GetMapping("/burst")
    public ResponseEntity<Map<String, Object>> burstEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "This endpoint allows bursts: 50 requests per 10 seconds",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "This endpoint has no rate limit annotation",
                "timestamp", Instant.now().toString()
        ));
    }
}
