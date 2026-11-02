package com.apisentinel;

import com.apisentinel.config.RateLimitProperties;
import com.apisentinel.exception.TooManyRequestsException;
import com.apisentinel.security.AuthRateLimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimiterTest {

    private static final Clock CLOCK =
            Clock.fixed(java.time.Instant.parse("2026-10-16T10:00:00Z"), ZoneOffset.UTC);

    private AuthRateLimiter limiter() {
        return new AuthRateLimiter(new RateLimitProperties(true, 10, 5, 5), CLOCK);
    }

    @Test
    @DisplayName("ek email par alag-alag IP se bhi 6th attempt block (distributed guessing)")
    void emailLimitAcrossIps() {
        AuthRateLimiter limiter = limiter();
        for (int i = 0; i < 5; i++) {
            limiter.checkLogin("10.0.0." + i, "Victim@x.com");
        }

        assertThatThrownBy(() -> limiter.checkLogin("10.0.0.99", "victim@X.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("ek IP se alag-alag emails par 11th attempt block (credential spraying)")
    void ipLimitAcrossEmails() {
        AuthRateLimiter limiter = limiter();
        for (int i = 0; i < 10; i++) {
            limiter.checkLogin("1.2.3.4", "user" + i + "@x.com");
        }

        assertThatThrownBy(() -> limiter.checkLogin("1.2.3.4", "user99@x.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("register: ek IP se 6th account par block")
    void registerLimit() {
        AuthRateLimiter limiter = limiter();
        for (int i = 0; i < 5; i++) {
            limiter.checkRegister("1.2.3.4");
        }

        assertThatThrownBy(() -> limiter.checkRegister("1.2.3.4"))
                .isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> limiter.checkRegister("5.6.7.8")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("disabled ho toh kuch block nahi hota")
    void disabled() {
        AuthRateLimiter limiter = new AuthRateLimiter(new RateLimitProperties(false, 1, 1, 1), CLOCK);

        assertThatCode(() -> {
            for (int i = 0; i < 50; i++) limiter.checkLogin("1.2.3.4", "a@x.com");
        }).doesNotThrowAnyException();
    }
}
