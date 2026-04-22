package org.example.controller;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GatewayFallbackController {

    @RequestMapping("/fallback/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> serviceFallback(@PathVariable String service,
                                                                     ServerWebExchange exchange) {
        Throwable throwable = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        String reason = throwable == null ? "upstream unavailable" : throwable.getClass().getSimpleName();

        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (!StringUtils.hasText(traceId)) {
            traceId = exchange.getResponse().getHeaders().getFirst("X-Trace-Id");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 503);
        body.put("message", "Gateway degraded for " + service + ", please retry later");
        body.put("data", null);
        body.put("service", service);
        body.put("reason", reason);
        body.put("traceId", traceId);
        body.put("timestamp", Instant.now().toString());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
