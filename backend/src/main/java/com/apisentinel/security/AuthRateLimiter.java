package com.apisentinel.security;

import com.apisentinel.config.RateLimitProperties;
import com.apisentinel.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

@Component
public class AuthRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimiter.class);

    private final boolean enabled;
    private final TokenBucketLimiter loginByIp;
    private final TokenBucketLimiter loginByEmail;
    private final TokenBucketLimiter registerByIp;

    public AuthRateLimiter(RateLimitProperties props, Clock clock) {
        this.enabled = props.enabled();
        this.loginByIp = new TokenBucketLimiter(props.loginIpPerMinute(), Duration.ofMinutes(1), clock);
        this.loginByEmail = new TokenBucketLimiter(props.loginEmailPerMinute(), Duration.ofMinutes(1), clock);
        this.registerByIp = new TokenBucketLimiter(props.registerIpPerHour(), Duration.ofHours(1), clock);
        if (!enabled) {
            log.warn("Auth rate limiting is DISABLED. Never run production like this.");
        }
    }

    public void checkLogin(String ip, String email) {
        if (!enabled) return;
        enforce("login-ip", loginByIp.tryAcquire(ip));
        enforce("login-email", loginByEmail.tryAcquire(email.trim().toLowerCase(Locale.ROOT)));
    }

    public void checkRegister(String ip) {
        if (!enabled) return;
        enforce("register-ip", registerByIp.tryAcquire(ip));
    }

    private void enforce(String rule, TokenBucketLimiter.Decision decision) {
        if (!decision.allowed()) {
            // Sirf rule ka naam log hota hai, email ya IP nahi (personal data)
            log.warn("Rate limit hit rule={} retryAfterSeconds={}", rule, decision.retryAfterSeconds());
            throw new TooManyRequestsException(decision.retryAfterSeconds());
        }
    }
}