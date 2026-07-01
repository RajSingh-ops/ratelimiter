<div align="center">
  <h1>🛡️ Spring Boot Rate Limiter</h1>
  <p><b>A production-grade, highly configurable distributed rate limiter for Spring Boot 3+</b></p>
  
  <a href="https://jitpack.io/#RajSingh-ops/ratelimiter">
    <img src="https://jitpack.io/v/RajSingh-ops/ratelimiter.svg" alt="JitPack Release" />
  </a>
  <a href="https://github.com/RajSingh-ops/ratelimiter/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT" />
  </a>
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg" alt="Spring Boot 3+" />
  </a>
  <a href="https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html">
    <img src="https://img.shields.io/badge/Java-17%2B-orange.svg" alt="Java 17+" />
  </a>
</div>

<br/>

Protect your APIs from abuse, DDoS attacks, and traffic spikes with zero boilerplate code. This library automatically configures itself and intercepts requests globally or per-endpoint using 5 distinct industry-standard algorithms.

---

## ✨ Features

- **Zero Configuration**: Works out-of-the-box using Spring Boot Auto-Configuration.
- **5 Built-in Algorithms**: 
  - `TOKEN_BUCKET` 🪣 (Default)
  - `LEAKY_BUCKET` 💧
  - `FIXED_WINDOW` ⏱️
  - `SLIDING_WINDOW_LOG` 📜
  - `SLIDING_WINDOW_COUNTER` 🧮
- **Annotation Driven**: Protect endpoints cleanly with `@RateLimit`.
- **Modern Stack**: Built natively for Java 17 and Spring Boot 3+.

---

## 📦 Installation

This library is hosted on [JitPack](https://jitpack.io).

**1. Add the JitPack repository to your `pom.xml`:**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

**2. Add the dependency:**
```xml
<dependency>
    <groupId>com.github.RajSingh-ops</groupId>
    <artifactId>ratelimiter</artifactId>
    <version>main-SNAPSHOT</version> <!-- Replace with specific release tag if desired -->
</dependency>
```

---

## 🚦 Quick Start

Add the `@RateLimit` annotation to any REST controller method to instantly protect it using your default configuration.

```java
import com.example.ratelimiter.annotation.RateLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

    // Instantly protected!
    @RateLimit
    @GetMapping("/api/data")
    public String getData() {
        return "This is protected data!";
    }
}
```

### 🎯 Endpoint-Specific Limits
You can override your global configuration for specific high-traffic endpoints by passing parameters directly to the annotation:

```java
// Allows 5 requests every 10 seconds specifically for this endpoint
@RateLimit(limit = 5, windowSeconds = 10, keyPrefix = "heavy-computation")
@GetMapping("/api/heavy")
public String getHeavyData() {
    return "Heavy data processed.";
}
```

---

## ⚙️ Global Configuration

Customize the global rate limiting engine in your `application.yml` or `application.properties`. If a configuration is missing, the library falls back to safe default values.

```yaml
ratelimiter:
  # The core math algorithm to use
  algorithm: SLIDING_WINDOW_COUNTER # Options: FIXED_WINDOW, SLIDING_WINDOW_LOG, SLIDING_WINDOW_COUNTER, TOKEN_BUCKET, LEAKY_BUCKET
  
  # Base settings (Window-based algorithms)
  max-requests: 100                 # Maximum requests allowed
  window-size: 1m                   # Time window (e.g., 1s, 1m, 1h)
  
  # Advanced settings (Bucket-based algorithms)
  bucket-capacity: 100              # Maximum tokens available (Token Bucket)
  refill-rate: 10                   # Tokens refilled per second (Token Bucket)
  leak-rate: 10                     # Requests processed per second (Leaky Bucket)
```

---

## 🧠 How it works under the hood
The library utilizes a Spring `HandlerInterceptor` that intercepts requests before they execute your controller logic. It extracts the client identifier (IP address by default) and validates the request against the configured `RateLimiter` algorithm in memory. If the limit is exceeded, it short-circuits the request and immediately returns a `429 Too Many Requests` HTTP status code, keeping your servers completely safe.

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
