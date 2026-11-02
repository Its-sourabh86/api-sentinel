package com.apisentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "sentinel.ratelimit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("10") int loginIpPerMinute,
        @DefaultValue("5") int loginEmailPerMinute,
        @DefaultValue("5") int registerIpPerHour
) {
    public RateLimitProperties {
        if (loginIpPerMinute < 1 || loginEmailPerMinute < 1 || registerIpPerHour < 1) {
            throw new IllegalArgumentException("rate limits must be at least 1");
        }
    }
}