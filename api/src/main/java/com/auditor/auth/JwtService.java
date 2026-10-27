package com.auditor.auth;

import com.auditor.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final long ACCESS_TOKEN_SECONDS = 900;

    private final String secret;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.issuer:ai-code-quality-auditor}") String issuer) {
        this.secret = secret;
        this.issuer = issuer;
    }

    public String generateAccessToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(ACCESS_TOKEN_SECONDS);

        return Jwts.builder()
                .subject(user.getUserId().toString())
                .claim("role", user.getRole().name())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public UUID validateAndGetUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("JWT subject is missing.");
        }
        if (claims.getExpiration() == null) {
            throw new JwtException("JWT expiration is missing.");
        }
        if (claims.getIssuedAt() == null) {
            throw new JwtException("JWT issued-at time is missing.");
        }

        Object role = claims.get("role");
        if (!(role instanceof String roleName)
                || !isKnownRole(roleName)) {
            throw new JwtException("JWT role claim is invalid.");
        }

        return UUID.fromString(subject);
    }

    private boolean isKnownRole(String roleName) {
        for (AppUser.Role role : AppUser.Role.values()) {
            if (role.name().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    private SecretKey getSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set the APP_JWT_SECRET environment variable.");
        }

        byte[] keyBytes;

        try {
            keyBytes = java.util.Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET must be a Base64-encoded random key.", ex);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET must decode to at least 32 bytes.");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
