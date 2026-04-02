package org.example.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    
    private static final String SECRET_KEY = "volunteerPlatformSecretKey2024ForJWTTokenGeneration";
    private static final long EXPIRATION_TIME = 86400000; // 24小时（单位：毫秒）
    // 可选：测试时可以设置为更短的时间，例如 5 分钟 = 300000 毫秒
    // private static final long EXPIRATION_TIME = 300000;
    
    private static Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }
    
    public static String generateToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
    
    public static String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }
    
    public static Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }
    
    public static String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }
}
