package org.example.mcp.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.mcp.auth.McpJwtClaims;
import org.example.mcp.auth.McpRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String traceId = resolveTraceId(request.getHeader(TRACE_HEADER));

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            McpJwtClaims claims = currentClaims(request);
            log.info(
                    "request completed method={} path={} status={} durationMs={} userId={} username={} role={} remoteIp={}",
                    request.getMethod(),
                    buildRequestPath(request),
                    response.getStatus(),
                    durationMs,
                    claims != null && claims.userId() != null ? claims.userId() : headerOrDefault(request, "X-User-Id"),
                    claims != null && StringUtils.hasText(claims.username()) ? claims.username() : headerOrDefault(request, "X-Username"),
                    claims != null && StringUtils.hasText(claims.role()) ? claims.role() : headerOrDefault(request, "X-User-Role"),
                    request.getRemoteAddr()
            );
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String resolveTraceId(String traceIdHeader) {
        return StringUtils.hasText(traceIdHeader) ? traceIdHeader : UUID.randomUUID().toString();
    }

    private String buildRequestPath(HttpServletRequest request) {
        if (!StringUtils.hasText(request.getQueryString())) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + request.getQueryString();
    }

    private McpJwtClaims currentClaims(HttpServletRequest request) {
        Object claims = request.getAttribute(McpRequestContext.ATTR_CLAIMS);
        return claims instanceof McpJwtClaims mcpJwtClaims ? mcpJwtClaims : null;
    }

    private String headerOrDefault(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return StringUtils.hasText(value) ? value : "-";
    }
}
