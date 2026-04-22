package org.example.mcp.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpJwtTokenServiceTest {

    private static final String SECRET = "12345678901234567890123456789012";

    @Test
    void parseShouldReturnGatewayClaims() {
        McpJwtTokenService tokenService = new McpJwtTokenService(SECRET);
        String token = Jwts.builder()
                .claim("userId", 7L)
                .claim("username", "admin")
                .claim("role", "ADMIN")
                .setExpiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        McpJwtClaims claims = tokenService.parse(token);

        assertEquals(token, claims.rawToken());
        assertEquals(7L, claims.userId());
        assertEquals("admin", claims.username());
        assertEquals("ADMIN", claims.role());
    }

    @Test
    void parseShouldRejectExpiredToken() {
        McpJwtTokenService tokenService = new McpJwtTokenService(SECRET);
        String token = Jwts.builder()
                .claim("userId", 7L)
                .claim("username", "admin")
                .claim("role", "ADMIN")
                .setExpiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tokenService.parse(token));

        assertEquals("access token is invalid", ex.getMessage());
    }
}
