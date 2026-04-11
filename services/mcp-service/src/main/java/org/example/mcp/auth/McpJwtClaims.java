package org.example.mcp.auth;

import java.time.Instant;

public record McpJwtClaims(
        String rawToken,
        Long userId,
        String username,
        String role,
        Instant expiresAt
) {
}
