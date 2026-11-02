package com.apisentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

@ConfigurationProperties(prefix = "sentinel.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);

        for (String origin : allowedOrigins) {
            if (origin.contains("*")) {
                throw new IllegalArgumentException(
                        "wildcard origins are not allowed in sentinel.cors.allowed-origins");
            }
            URI uri = URI.create(origin);
            boolean validScheme = "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
            boolean noPath = uri.getPath() == null || uri.getPath().isEmpty();
            if (!validScheme || uri.getHost() == null || !noPath) {
                throw new IllegalArgumentException(
                        "each origin must look like http(s)://host[:port] with no path");
            }
        }
    }
}