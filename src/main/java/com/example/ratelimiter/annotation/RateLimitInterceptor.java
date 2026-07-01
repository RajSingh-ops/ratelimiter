package com.example.ratelimiter.annotation;

import com.example.ratelimiter.config.RateLimiterProperties;
import com.example.ratelimiter.dto.RateLimitResponse;
import com.example.ratelimiter.model.RateLimitResult;
import com.example.ratelimiter.service.KeyResolver;
import com.example.ratelimiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimiterService rateLimiterService;
    private final KeyResolver keyResolver;
    private final RateLimiterProperties properties;

    public RateLimitInterceptor(RateLimiterService rateLimiterService,
                                KeyResolver keyResolver,
                                RateLimiterProperties properties) {
        this.rateLimiterService = rateLimiterService;
        this.keyResolver = keyResolver;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        if (rateLimit == null && !properties.isGlobalEnabled()) {
            return true;
        }

        String clientKey = keyResolver.resolve(request);
        String key = buildKey(rateLimit, request, clientKey);

        RateLimitResult result;
        if (rateLimit != null && rateLimit.limit() > 0 && rateLimit.windowSeconds() > 0) {
            result = rateLimiterService.check(key, rateLimit.limit(), rateLimit.windowSeconds());
        } else {
            result = rateLimiterService.check(key);
        }

        setRateLimitHeaders(response, result);

        if (!result.allowed()) {
            log.debug("Rate limit exceeded for key: {}", key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            writeJsonResponse(response, RateLimitResponse.from(result));
            return false;
        }

        return true;
    }

    private String buildKey(RateLimit rateLimit, HttpServletRequest request, String clientKey) {
        String prefix;
        if (rateLimit != null && !rateLimit.keyPrefix().isEmpty()) {
            prefix = rateLimit.keyPrefix();
        } else {
            prefix = request.getRequestURI();
        }
        return prefix + ":" + clientKey;
    }

    private void setRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.currentLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingRequests()));
        response.setHeader("X-RateLimit-Algorithm", result.algorithm().getDisplayName());

        if (!result.allowed()) {
            long retryAfterSeconds = Math.max(1, result.retryAfter().toSeconds());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        }
    }

    private void writeJsonResponse(HttpServletResponse response, RateLimitResponse dto) throws Exception {
        PrintWriter writer = response.getWriter();
        writer.write("{");
        writer.write("\"allowed\":" + dto.allowed() + ",");
        writer.write("\"remainingRequests\":" + dto.remainingRequests() + ",");
        writer.write("\"retryAfterMs\":" + dto.retryAfterMs() + ",");
        writer.write("\"currentLimit\":" + dto.currentLimit() + ",");
        writer.write("\"algorithm\":\"" + escapeJson(dto.algorithm()) + "\",");
        writer.write("\"timestamp\":\"" + escapeJson(dto.timestamp()) + "\"");
        writer.write("}");
        writer.flush();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
