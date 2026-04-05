package org.example.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
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
            log.info(
                    "request completed method={} path={} status={} durationMs={} remoteIp={}",
                    request.getMethod(),
                    buildRequestPath(request),
                    response.getStatus(),
                    durationMs,
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
}
