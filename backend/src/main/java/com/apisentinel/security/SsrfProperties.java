package com.apisentinel.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "sentinel.security.ssrf")
public record SsrfProperties(
        @DefaultValue("false") boolean allowPrivateTargets,
        @DefaultValue("0") int maxRedirects
) {
    public SsrfProperties {
        if (maxRedirects < 0 || maxRedirects > 10) {
            throw new IllegalArgumentException("max-redirects must be between 0 and 10");
        }
    }
}