package com.apisentinel.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Clock;
import java.time.Duration;

public class TokenBucketLimiter {

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private final double capacity;
    private final double refillPerMs;
    private final Clock clock;

    // Memory bounded: max size aur expiry. Plain map hota toh attacker lakhon keys se bhar deta.
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    /** "permits" requests per "period". Bucket ki capacity = permits. */
    public TokenBucketLimiter(int permits, Duration period, Clock clock) {
        this.capacity = permits;
        this.refillPerMs = permits / (double) period.toMillis();
        this.clock = clock;
    }

    public Decision tryAcquire(String key) {
        long now = clock.millis();
        Bucket bucket = buckets.get(key, k -> new Bucket(capacity, now));
        return bucket.take(now, capacity, refillPerMs);
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillMs;

        Bucket(double tokens, long nowMs) {
            this.tokens = tokens;
            this.lastRefillMs = nowMs;
        }

        // synchronized: ek key ki do requests ek saath aayein toh bhi ginti sahi rahe
        synchronized Decision take(long nowMs, double capacity, double refillPerMs) {
            long elapsed = Math.max(0, nowMs - lastRefillMs);
            tokens = Math.min(capacity, tokens + elapsed * refillPerMs);
            lastRefillMs = nowMs;

            if (tokens >= 1) {
                tokens -= 1;
                return new Decision(true, 0);
            }
            double msUntilNextToken = (1 - tokens) / refillPerMs;
            long seconds = Math.max(1, (long) Math.ceil(msUntilNextToken / 1000.0));
            return new Decision(false, seconds);
        }
    }
}