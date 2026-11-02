package com.apisentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;

@ConfigurationProperties(prefix = "sentinel.notification")
public record NotificationProperties(
        @DefaultValue("false") boolean enabled,
        String webhookUrl,
        String webhookSecret,
        @DefaultValue("5000") int timeoutMs,
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("1000") long retryBackoffMs
) {
    public NotificationProperties {
        if (maxAttempts < 1 || timeoutMs < 1 || retryBackoffMs < 0) {
            throw new IllegalArgumentException("invalid notification settings");
        }
        if (enabled) {
            if (webhookUrl == null || webhookUrl.isBlank()) {
                throw new IllegalArgumentException("webhook-url is required when notifications are enabled");
            }
            String scheme = URI.create(webhookUrl).getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("webhook-url must use http or https");
            }
        }
    }
}
