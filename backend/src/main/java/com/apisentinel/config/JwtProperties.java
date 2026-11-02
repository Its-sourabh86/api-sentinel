package com.apisentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "sentinel.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("60") long expiryMinutes
) {
    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // Secret ki value error message mein kabhi nahi
            throw new IllegalArgumentException("sentinel.jwt.secret must be at least 32 bytes long");
        }
        if (expiryMinutes < 1 || expiryMinutes > 1440) {
            throw new IllegalArgumentException("sentinel.jwt.expiry-minutes must be between 1 and 1440");
        }
    }
}