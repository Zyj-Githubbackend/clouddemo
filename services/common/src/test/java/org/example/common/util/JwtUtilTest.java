package org.example.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    @Test
    void generateTokenShouldContainExpectedClaims() {
        String token = JwtUtil.generateToken("student01", 123L, "VOLUNTEER");

        Claims claims = JwtUtil.parseToken(token);

        assertEquals("student01", claims.getSubject());
        assertEquals(123L, JwtUtil.getUserIdFromToken(token));
        assertEquals("VOLUNTEER", JwtUtil.getRoleFromToken(token));
        assertTrue(JwtUtil.validateToken(token));
    }

    @Test
    void validateTokenShouldReturnFalseForTamperedToken() {
        String token = JwtUtil.generateToken("student01", 123L, "VOLUNTEER");
        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        assertFalse(JwtUtil.validateToken(tamperedToken));
    }
}
