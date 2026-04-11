package org.example.mcp.auth;

public record LoginRequest(
        String username,
        String password
) {
}
