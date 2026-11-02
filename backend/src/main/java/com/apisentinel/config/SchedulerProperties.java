package com.apisentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "sentinel.scheduler")
public record SchedulerProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("5000") long tickMs,
        @DefaultValue("50") int batchSize,
        @DefaultValue("10") int workerThreads,
        @DefaultValue("100") int queueCapacity
) {
}
