package com.attendance.config;

import com.attendance.entity.Role;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtUtil {

    private final String SECRET = "my-super-secret-key-123456789123456789";

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String cleanToken(String token) {
        if (token == null) return null;

        if (token.startsWith("Bearer ")) {
            return token.substring(7).trim();
        }
        return token.trim();
    }

    public String generateToken(String name, String email, Role role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role) // 🔥 add role
                .claim("username", name)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        token = cleanToken(token); // 🔥 REMOVE "Bearer "
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractRole(String token) {
        token = cleanToken(token); // 🔥 REMOVE "Bearer "
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public String extractUsername(String token) {
        token = cleanToken(token); // 🔥 REMOVE "Bearer "
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("username", String.class);
    }
}
