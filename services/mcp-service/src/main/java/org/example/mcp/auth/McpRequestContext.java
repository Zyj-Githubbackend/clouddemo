package org.example.mcp.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class McpRequestContext {

    public static final String ATTR_CLAIMS = "cloudDemoMcpClaims";

    public McpJwtClaims requireClaims() {
        McpJwtClaims claims = currentClaims();
        if (claims == null) {
            throw new IllegalStateException("No authenticated MCP session");
        }
        return claims;
    }

    public McpJwtClaims currentClaims() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object claims = request.getAttribute(ATTR_CLAIMS);
        return claims instanceof McpJwtClaims mcpJwtClaims ? mcpJwtClaims : null;
    }

    public String requireGatewayToken() {
        return requireClaims().rawToken();
    }

    public void requireAdmin() {
        String role = requireClaims().role();
        if (!"ADMIN".equals(role)) {
            throw new IllegalStateException("Admin role is required");
        }
    }
}
