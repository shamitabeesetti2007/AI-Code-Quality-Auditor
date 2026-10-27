
package com.auditor.auth;

import com.auditor.auth.dto.LoginRequest;
import com.auditor.auth.dto.LoginResponse;
import com.auditor.auth.dto.RegisterRequest;
import com.auditor.auth.dto.UserResponse;
import com.auditor.auth.token.InvalidRefreshTokenException;
import com.auditor.auth.token.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final Set<String> allowedOrigins;
    private final boolean secureCookie;

    public AuthController(
            AuthService authService,
            RefreshTokenService refreshTokenService,
            @Value("${app.security.allowed-origins:http://localhost:5173}")
            String configuredOrigins,
            @Value("${app.security.cookie-secure:${APP_COOKIE_SECURE:true}}")
            boolean secureCookie) {

        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.allowedOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResult result = authService.login(
                request, httpRequest.getRemoteAddr());

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(
                        HttpHeaders.SET_COOKIE,
                        buildRefreshCookie(
                                result.refreshToken(),
                                result.refreshTokenExpiresAt()))
                .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(
                    name = REFRESH_COOKIE,
                    required = false) String rawRefreshToken,
            HttpServletRequest request) {

        if (!hasAllowedOrigin(request)) {
            return problem(
                    HttpStatus.FORBIDDEN,
                    "Request rejected by security policy.");
        }

        try {
            var rotated = refreshTokenService.rotate(rawRefreshToken);
            LoginResponse response = LoginResponse.from(
                    rotated.accessToken(),
                    UserResponse.from(rotated.user()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(
                            HttpHeaders.SET_COOKIE,
                            buildRefreshCookie(
                                    rotated.rawToken(),
                                    rotated.expiresAt()))
                    .body(response);

        } catch (InvalidRefreshTokenException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie())
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(Map.of(
                            "type", "about:blank",
                            "title", "Unauthorized",
                            "status", 401,
                            "detail", "Refresh token is invalid or expired."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(
                    name = REFRESH_COOKIE,
                    required = false) String rawRefreshToken,
            HttpServletRequest request) {

        if (!hasAllowedOrigin(request)) {
            return problem(
                    HttpStatus.FORBIDDEN,
                    "Request rejected by security policy.");
        }

        // Logout is idempotent, including when the cookie is absent. Clear
        // the browser token even if server-side family revocation fails, but
        // preserve that failure in the response.
        try {
            refreshTokenService.logout(rawRefreshToken);
        } catch (RuntimeException ex) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred.");
            problem.setType(URI.create("about:blank"));
            return ResponseEntity.internalServerError()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie())
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problem);
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie())
                .build();
    }

    private boolean hasAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);

        if (origin == null || origin.isBlank()) {
            return false;
        }

        // Require a syntactically valid origin without a path, query,
        // fragment, or user-info component.
        try {
            URI uri = URI.create(origin);

            if (uri.getScheme() == null
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || (uri.getRawPath() != null
                        && !uri.getRawPath().isEmpty()
                        && !"/".equals(uri.getRawPath()))) {
                return false;
            }

            if (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
        } catch (IllegalArgumentException ex) {
            return false;
        }

        // Exact configured-origin match; do not use substring matching.
        return allowedOrigins.contains(origin);
    }

    private String buildRefreshCookie(
            String rawToken,
            Instant expiresAt) {

        long maxAge = Math.max(
                0,
                Duration.between(Instant.now(), expiresAt).getSeconds());

        return ResponseCookie.from(REFRESH_COOKIE, rawToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }

    private String clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build()
                .toString();
    }

    private ResponseEntity<Map<String, Object>> problem(
            HttpStatus status,
            String detail) {

        return ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Map.of(
                        "type", "about:blank",
                        "title", status.getReasonPhrase(),
                        "status", status.value(),
                        "detail", detail));
    }
}
