package org.example.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
public class GatewayResilienceConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> gatewayCircuitBreakerCustomizer(
            @Value("${spring.cloud.gateway.httpclient.response-timeout:35s}") Duration responseTimeout,
            @Value("${resilience4j.circuitbreaker.configs.gatewayDefault.slow-call-duration-threshold:30s}")
            Duration slowCallThreshold) {

        Duration effectiveTimeout = responseTimeout == null || responseTimeout.isNegative() || responseTimeout.isZero()
                ? Duration.ofSeconds(35)
                : responseTimeout;
        Duration effectiveSlowCallThreshold = slowCallThreshold == null
                || slowCallThreshold.isNegative()
                || slowCallThreshold.isZero()
                ? Duration.ofSeconds(30)
                : slowCallThreshold;

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .slowCallDurationThreshold(effectiveSlowCallThreshold)
                .slowCallRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(5)
                .build();

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(effectiveTimeout)
                .cancelRunningFuture(true)
                .build();

        return factory -> {
            factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(circuitBreakerConfig)
                    .timeLimiterConfig(timeLimiterConfig)
                    .build());
            factory.configure(builder -> builder
                            .circuitBreakerConfig(circuitBreakerConfig)
                            .timeLimiterConfig(timeLimiterConfig)
                            .build(),
                    "userServiceCircuitBreaker",
                    "activityServiceCircuitBreaker",
                    "announcementServiceCircuitBreaker",
                    "feedbackServiceCircuitBreaker");
        };
    }

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
