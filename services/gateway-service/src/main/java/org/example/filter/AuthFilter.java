package org.example.filter;

import org.example.common.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/activity/list",
            "/activity/image"
    );
    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String traceId = resolveTraceId(request.getHeaders().getFirst(TRACE_HEADER));

        ServerHttpRequest tracedRequest = request.mutate()
                .header(TRACE_HEADER, traceId)
                .build();
        ServerWebExchange tracedExchange = exchange.mutate().request(tracedRequest).build();
        tracedExchange.getResponse().getHeaders().set(TRACE_HEADER, traceId);

        String path = tracedRequest.getURI().getPath();

        if (isWhiteListed(path)) {
            return chain.filter(tracedExchange)
                    .doFinally(signalType -> logRequest(tracedExchange, startTime, traceId, null, null));
        }

        String token = tracedRequest.getHeaders().getFirst("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("gateway rejected request because token is missing path={} traceId={}", path, traceId);
            return unauthorizedResponse(tracedExchange, traceId, "鏈彁渚涜璇佷护鐗?");
        }

        token = token.substring(7);

        if (!JwtUtil.validateToken(token)) {
            log.warn("gateway rejected request because token is invalid path={} traceId={}", path, traceId);
            return unauthorizedResponse(tracedExchange, traceId, "璁よ瘉浠ょ墝鏃犳晥鎴栧凡杩囨湡");
        }

        try {
            Long userId = JwtUtil.getUserIdFromToken(token);
            String username = JwtUtil.getUsernameFromToken(token);
            String role = JwtUtil.getRoleFromToken(token);

            ServerHttpRequest mutatedRequest = tracedRequest.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(tracedExchange.mutate().request(mutatedRequest).build())
                    .doFinally(signalType -> logRequest(tracedExchange, startTime, traceId, userId, role));

        } catch (Exception e) {
            log.warn("gateway rejected request because token parsing failed path={} traceId={}", path, traceId, e);
            return unauthorizedResponse(tracedExchange, traceId, "浠ょ墝瑙ｆ瀽澶辫触");
        }
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String traceId, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        response.getHeaders().set(TRACE_HEADER, traceId);

        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    private void logRequest(ServerWebExchange exchange, long startTime, String traceId, Long userId, String role) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator")) {
            return;
        }
        long durationMs = System.currentTimeMillis() - startTime;
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        log.info(
                "gateway request completed method={} path={} status={} durationMs={} traceId={} userId={} role={}",
                exchange.getRequest().getMethod(),
                path,
                status != null ? status.value() : 200,
                durationMs,
                traceId,
                userId != null ? userId : "-",
                StringUtils.hasText(role) ? role : "-"
        );
    }

    private String resolveTraceId(String traceIdHeader) {
        return StringUtils.hasText(traceIdHeader) ? traceIdHeader : UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
