package org.example.mcp.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpAuthorizationCodeStore {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final Map<String, McpAuthorizationCode> codes = new ConcurrentHashMap<>();

    public McpAuthorizationCode issue(
            String clientId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String scope,
            String gatewayToken,
            String username,
            String role
    ) {
        String code = UUID.randomUUID().toString().replace("-", "");
        McpAuthorizationCode authorizationCode = new McpAuthorizationCode(
                code,
                clientId,
                redirectUri,
                codeChallenge,
                codeChallengeMethod,
                scope,
                gatewayToken,
                username,
                role,
                Instant.now().plus(CODE_TTL)
        );
        codes.put(code, authorizationCode);
        return authorizationCode;
    }

    public McpAuthorizationCode consume(String code) {
        McpAuthorizationCode authorizationCode = codes.remove(code);
        if (authorizationCode == null || authorizationCode.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("authorization code is invalid or expired");
        }
        return authorizationCode;
    }
}
