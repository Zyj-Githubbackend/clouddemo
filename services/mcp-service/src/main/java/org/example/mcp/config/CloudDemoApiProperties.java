package org.example.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cloud.demo.api")
public record CloudDemoApiProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
