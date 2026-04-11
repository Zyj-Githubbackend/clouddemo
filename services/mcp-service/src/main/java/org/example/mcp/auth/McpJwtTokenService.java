package org.example.mcp.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class McpJwtTokenService {

    private final Key signingKey;

    public McpJwtTokenService(@Value("${cloud.demo.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public McpJwtClaims parse(String rawToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(rawToken)
                    .getBody();

            Date expiration = claims.getExpiration();
            if (expiration == null || expiration.before(new Date())) {
                throw new IllegalArgumentException("access token is expired");
            }

            return new McpJwtClaims(
                    rawToken,
                    claims.get("userId", Long.class),
                    claims.get("username", String.class),
                    claims.get("role", String.class),
                    expiration.toInstant()
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("access token is invalid", ex);
        }
    }
}
