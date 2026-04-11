package org.example.mcp.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, CloudDemoApiProperties properties) {
        return builder
                .connectTimeout(properties.connectTimeout())
                .readTimeout(properties.readTimeout())
                .build();
    }
}
