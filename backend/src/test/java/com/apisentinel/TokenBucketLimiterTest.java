package com.apisentinel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.apisentinel.security.TokenBucketLimiter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketLimiterTest {

    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-16T10:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    // 3 requests per minute: har 20 second mein ek token wapas
    private final TokenBucketLimiter limiter = new TokenBucketLimiter(3, Duration.ofMinutes(1), clock);

    @Test
    @DisplayName("capacity tak allow, uske baad block aur retry-after sahi")
    void blocksAfterCapacity() {
        assertThat(limiter.tryAcquire("k").allowed()).isTrue();
        assertThat(limiter.tryAcquire("k").allowed()).isTrue();
        assertThat(limiter.tryAcquire("k").allowed()).isTrue();

        TokenBucketLimiter.Decision blocked = limiter.tryAcquire("k");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(20);
    }

    @Test
    @DisplayName("waqt guzarne par token wapas bharta hai")
    void refillsOverTime() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("k");
        assertThat(limiter.tryAcquire("k").allowed()).isFalse();

        clock.advance(Duration.ofSeconds(20));

        assertThat(limiter.tryAcquire("k").allowed()).isTrue();
        assertThat(limiter.tryAcquire("k").allowed()).isFalse();
    }

    @Test
    @DisplayName("bahut der baad bhi capacity se zyada token jama nahi hote")
    void neverExceedsCapacity() {
        clock.advance(Duration.ofHours(5));

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("k").allowed()).isTrue();
        }
        assertThat(limiter.tryAcquire("k").allowed()).isFalse();
    }

    @Test
    @DisplayName("alag keys ek dusre ko affect nahi karti")
    void keysAreIndependent() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("a");

        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
        assertThat(limiter.tryAcquire("b").allowed()).isTrue();
    }
}