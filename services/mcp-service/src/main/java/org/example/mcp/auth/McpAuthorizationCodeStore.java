package org.example.mcp.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class McpAuthorizationCodeStore {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public McpAuthorizationCodeStore(StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${cloud.demo.mcp.authorization-code-prefix:mcp:authorization-code:}") String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix;
    }

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
        redisTemplate.opsForValue().set(redisKey(code), writeAuthorizationCode(authorizationCode), CODE_TTL);
        return authorizationCode;
    }

    public McpAuthorizationCode consume(String code) {
        String rawCode = redisTemplate.opsForValue().getAndDelete(redisKey(code));
        McpAuthorizationCode authorizationCode = readAuthorizationCode(rawCode);
        if (authorizationCode == null || authorizationCode.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("authorization code is invalid or expired");
        }
        return authorizationCode;
    }

    private String redisKey(String code) {
        return keyPrefix + code;
    }

    private String writeAuthorizationCode(McpAuthorizationCode authorizationCode) {
        try {
            return objectMapper.writeValueAsString(authorizationCode);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize MCP authorization code", ex);
        }
    }

    private McpAuthorizationCode readAuthorizationCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawCode, McpAuthorizationCode.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("authorization code is invalid or expired", ex);
        }
    }
}
