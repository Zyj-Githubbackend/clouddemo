package org.example.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayResilienceConfig {

    @Bean("ipOrUserKeyResolver")
    public KeyResolver ipOrUserKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (StringUtils.hasText(userId)) {
                return Mono.just("user:" + userId.trim());
            }

            String ipAddress = null;
            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                ipAddress = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            if (!StringUtils.hasText(ipAddress)) {
                ipAddress = "unknown";
            }
            return Mono.just("ip:" + ipAddress);
        };
    }
}
