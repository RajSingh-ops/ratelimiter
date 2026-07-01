package com.example.ratelimiter.dto;

import jakarta.validation.constraints.NotBlank;

public record RateLimitCheckRequest(

        @NotBlank(message = "Key must not be blank")
        String key
) {
}
