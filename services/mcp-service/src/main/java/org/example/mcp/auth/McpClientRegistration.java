package org.example.mcp.auth;

import java.time.Instant;
import java.util.List;

public record McpClientRegistration(
        String clientId,
        String clientName,
        List<String> redirectUris,
        Instant clientIdIssuedAt
) {
}
