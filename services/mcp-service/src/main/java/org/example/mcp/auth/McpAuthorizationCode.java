package org.example.mcp.auth;

import java.time.Instant;

public record McpAuthorizationCode(
        String code,
        String clientId,
        String redirectUri,
        String codeChallenge,
        String codeChallengeMethod,
        String scope,
        String gatewayToken,
        String username,
        String role,
        Instant expiresAt
) {
}
