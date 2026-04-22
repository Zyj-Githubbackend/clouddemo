package org.example.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${user.service.connect-timeout:2s}") Duration connectTimeout,
            @Value("${user.service.read-timeout:3s}") Duration readTimeout
    ) {
        return restTemplateBuilder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }
}
