package com.apisentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "sentinel.incident")
public record IncidentProperties(
        @DefaultValue("3") int failureThreshold,
        @DefaultValue("2") int recoveryThreshold
) {
    public IncidentProperties {
        if (failureThreshold < 1 || recoveryThreshold < 1) {
            throw new IllegalArgumentException("incident thresholds must be at least 1");
        }
    }
}
