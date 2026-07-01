package com.example.ratelimiter.controller;

import com.example.ratelimiter.annotation.RateLimit;
import com.example.ratelimiter.dto.RateLimitCheckRequest;
import com.example.ratelimiter.dto.RateLimitResponse;
import com.example.ratelimiter.model.RateLimitResult;
import com.example.ratelimiter.service.RateLimiterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/rate-limit")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> check(
            @Valid @RequestBody RateLimitCheckRequest request) {

        RateLimitResult result = rateLimiterService.check(request.key());
        RateLimitResponse response = RateLimitResponse.from(result);

        if (result.allowed()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(
                        Math.max(1, result.retryAfter().toSeconds())))
                .body(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Key is required",
                    "timestamp", Instant.now().toString()));
        }

        rateLimiterService.reset(key);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "reset");
        response.put("key", key);
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(rateLimiterService.getStats());
    }
}
