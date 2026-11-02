package com.apisentinel.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SsrfProperties.class)
public class SecurityConfig {
}
