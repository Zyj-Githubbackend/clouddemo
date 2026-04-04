package org.example.filter;

import org.example.common.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {
    
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/user/login",
        "/user/register",
        "/activity/list",
        "/activity/image"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }
        
        String token = request.getHeaders().getFirst("Authorization");
        
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, "未提供认证令牌");
        }
        
        token = token.substring(7);
        
        if (!JwtUtil.validateToken(token)) {
            return unauthorizedResponse(exchange, "认证令牌无效或已过期");
        }
        
        try {
            Long userId = JwtUtil.getUserIdFromToken(token);
            String username = JwtUtil.getUsernameFromToken(token);
            String role = JwtUtil.getRoleFromToken(token);
            
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username)
                    .header("X-User-Role", role)
                    .build();
            
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
            
        } catch (Exception e) {
            return unauthorizedResponse(exchange, "令牌解析失败");
        }
    }
    
    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }
    
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -100;
    }
}
