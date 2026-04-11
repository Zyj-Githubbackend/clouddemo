package org.example.mcp.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class McpAccessTokenFilter extends OncePerRequestFilter {

    private final McpJwtTokenService jwtTokenService;

    public McpAccessTokenFilter(McpJwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/mcp")) {
            return true;
        }
        return path.startsWith("/mcp/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(request, response, "invalid_token", "Bearer access token is required");
            return;
        }

        String token = authorization.substring(7).trim();
        try {
            McpJwtClaims claims = jwtTokenService.parse(token);
            request.setAttribute(McpRequestContext.ATTR_CLAIMS, claims);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            writeUnauthorized(request, response, "invalid_token", ex.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String error,
                                   String description) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                "Bearer realm=\"cloud-demo-mcp\", error=\"" + error
                        + "\", error_description=\"" + escape(description)
                        + "\", resource_metadata=\"" + protectedResourceMetadataUrl(request) + "\"");
        response.getWriter().write("{\"error\":\"" + escape(error) + "\",\"error_description\":\""
                + escape(description) + "\"}");
    }

    private String protectedResourceMetadataUrl(HttpServletRequest request) {
        return externalBaseUrl(request) + "/.well-known/oauth-protected-resource";
    }

    private String externalBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
