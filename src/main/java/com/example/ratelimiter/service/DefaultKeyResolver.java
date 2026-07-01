package com.example.ratelimiter.service;

import com.example.ratelimiter.config.RateLimiterProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class DefaultKeyResolver implements KeyResolver {

    private final RateLimiterProperties properties;

    public DefaultKeyResolver(RateLimiterProperties properties) {
        this.properties = properties;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        return switch (properties.getKeyStrategy()) {
            case IP -> resolveIp(request);
            case USER -> resolveUser(request);
            case API_KEY -> resolveApiKey(request);
            case HEADER -> resolveHeader(request);
        };
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveUser(HttpServletRequest request) {
        var principal = request.getUserPrincipal();
        if (principal != null) {
            return "user:" + principal.getName();
        }
        return resolveIp(request);
    }

    private String resolveApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "apikey:" + apiKey.trim();
        }
        return resolveIp(request);
    }

    private String resolveHeader(HttpServletRequest request) {
        String clientId = request.getHeader("X-Client-ID");
        if (clientId != null && !clientId.isBlank()) {
            return "client:" + clientId.trim();
        }
        return resolveIp(request);
    }
}
