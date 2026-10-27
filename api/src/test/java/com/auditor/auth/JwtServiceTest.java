
package com.auditor.auth;

import com.auditor.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private static final String ISSUER = "ai-code-quality-auditor";

    // Deterministic test-only key; never use this key in production.
    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString(
                    "test-only-key-with-at-least-32-bytes".getBytes(
                            StandardCharsets.UTF_8));

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, ISSUER);
    }

    @Test
    void generatedTokenContainsExpectedSubjectIssuerAndRole() {
        UUID userId = UUID.randomUUID();
        AppUser user = mock(AppUser.class);

        when(user.getUserId()).thenReturn(userId);
        when(user.getRole()).thenReturn(AppUser.Role.USER);

        String token = jwtService.generateAccessToken(user);

        assertEquals(userId, jwtService.validateAndGetUserId(token));

        Claims claims = Jwts.parser()
                .verifyWith(testSigningKey())
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("USER", claims.get("role"));
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void rejectsTokenWithWrongIssuer() {
        String token = createToken("another-issuer", Instant.now().plusSeconds(60));

        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.validateAndGetUserId(token));
    }

    @Test
    void rejectsExpiredToken() {
        String token = createToken(ISSUER, Instant.now().minusSeconds(60));

        assertThrows(
                io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.validateAndGetUserId(token));
    }

    @Test
    void rejectsSignedTokenWithMissingSubject() {
        String token = Jwts.builder()
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(testSigningKey())
                .compact();

        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.validateAndGetUserId(token));
    }

    @Test
    void rejectsSignedTokenWithBlankSubject() {
        String token = Jwts.builder()
                .subject(" ")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(testSigningKey())
                .compact();

        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.validateAndGetUserId(token));
    }

    @Test
    void rejectsSignedTokenWithMissingExpiration() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "USER")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .signWith(testSigningKey())
                .compact();

        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.validateAndGetUserId(token));
    }

    @Test
    void rejectsSignedTokenWithMissingIssuedAt() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "USER")
                .issuer(ISSUER)
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(testSigningKey())
                .compact();

        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.validateAndGetUserId(token));
    }

    @Test
    void rejectsSignedTokenWithMissingRole() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(testSigningKey())
                .compact();

        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.validateAndGetUserId(token));
    }

    @Test
    void rejectsSignedTokenWithUnknownRole() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "SUPERUSER")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(testSigningKey())
                .compact();

        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.validateAndGetUserId(token));
    }

    @Test
    void rejectsMalformedToken() {
        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.validateAndGetUserId("not-a-valid-jwt"));
    }

    @Test
    void rejectsMissingSecret() {
        JwtService serviceWithoutSecret = new JwtService("", ISSUER);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> serviceWithoutSecret.validateAndGetUserId("any-token"));
        assertTrue(exception.getMessage().contains("APP_JWT_SECRET"));
    }

    @Test
    void rejectsSecretShorterThan32DecodedBytes() {
        String shortSecret = Base64.getEncoder()
                .encodeToString("too-short".getBytes(StandardCharsets.UTF_8));

        JwtService serviceWithShortSecret = new JwtService(shortSecret, ISSUER);

        assertThrows(
                IllegalStateException.class,
                () -> serviceWithShortSecret.validateAndGetUserId("any-token"));
    }

    private String createToken(String issuer, Instant expiry) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "USER")
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(testSigningKey())
                .compact();
    }

    private SecretKey testSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
    }
}
