package com.poker.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/** Tạo và xác thực JWT cho các lệnh WebSocket Java thuần. */
public final class JwtUtil {
    private static final long EXPIRATION_TIME = 86_400_000L;
    private static final Key SIGNING_KEY = createSigningKey();

    private JwtUtil() { }

    private static Key createSigningKey() {
        String configured = System.getenv("JWT_SECRET");
        if (configured == null || configured.isBlank()) {
            // A fresh key per server process is safe for local development. Set JWT_SECRET
            // on the server to keep access tokens valid across server restarts.
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
        return Keys.hmacShaKeyFor(configured.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(Long.toString(userId))
                .claim("username", username)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Long validateTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SIGNING_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (!"access".equals(claims.get("type", String.class))) return null;
            return Long.valueOf(claims.getSubject());
        } catch (RuntimeException invalidToken) {
            return null;
        }
    }
}
