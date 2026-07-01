package com.example.ratelimiter.service;

import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface KeyResolver {

    String resolve(HttpServletRequest request);
}
