
package com.auditor.auth;

import com.auditor.auth.dto.LoginRequest;
import com.auditor.auth.token.RefreshTokenService;
import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private LoginRateLimiter.Attempt loginAttempt;

    @InjectMocks
    private AuthService authService;

    private AppUser activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new AppUser(
                "test@example.com",
                "$2a$10$storedHash",
                "Test User");
    }

    @Test
    void loginReturnsAccessTokenAndRefreshTokenResultForValidCredentials() {
        when(loginRateLimiter.beginAttempt(anyString(), anyString()))
                .thenReturn(loginAttempt);
        LoginRequest request = new LoginRequest(
                " TEST@Example.com ",
                "correct-password");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(
                "correct-password", activeUser.getPasswordHash()))
                .thenReturn(true);
        when(refreshTokenService.issueInitial(activeUser))
                .thenReturn(new RefreshTokenService.IssuedToken(
                        "opaque-refresh-token",
                        Instant.now().plusSeconds(604800),
                        Instant.now().plusSeconds(2592000)));
        when(jwtService.generateAccessToken(activeUser))
                .thenReturn("test-access-token");

        AuthResult result = authService.login(request, "192.0.2.10");

        assertEquals("test-access-token",
                result.response().accessToken());
        assertEquals("Bearer", result.response().tokenType());
        assertEquals(900, result.response().expiresIn());
        assertEquals("test@example.com",
                result.response().user().email());
        assertEquals("opaque-refresh-token", result.refreshToken());
        assertNotNull(result.refreshTokenExpiresAt());

        verify(refreshTokenService).issueInitial(activeUser);
        verify(jwtService).generateAccessToken(activeUser);
        verify(loginRateLimiter).beginAttempt(
                "test@example.com", "192.0.2.10");
        verify(loginAttempt).recordSuccess();
    }

    @Test
    void loginRejectsIncorrectPassword() {
        when(loginRateLimiter.beginAttempt(anyString(), anyString()))
                .thenReturn(loginAttempt);
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "wrong-password");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(
                "wrong-password", activeUser.getPasswordHash()))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request, "192.0.2.10"));

        verify(jwtService, never()).generateAccessToken(any());
        verifyNoInteractions(refreshTokenService);
        verify(loginAttempt).recordFailure();
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(loginRateLimiter.beginAttempt(anyString(), anyString()))
                .thenReturn(loginAttempt);
        LoginRequest request = new LoginRequest(
                "unknown@example.com",
                "some-password");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.matches(
                eq("some-password"), any(String.class)))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request, "192.0.2.10"));

        verify(jwtService, never()).generateAccessToken(any());
        verifyNoInteractions(refreshTokenService);
        verify(loginAttempt).recordFailure();
    }

    @Test
    void loginRejectsDisabledAccount() {
        when(loginRateLimiter.beginAttempt(anyString(), anyString()))
                .thenReturn(loginAttempt);
        AppUser disabledUser = mock(AppUser.class);

        when(disabledUser.getAccountStatus())
                .thenReturn(AppUser.AccountStatus.DISABLED);
        when(disabledUser.getPasswordHash())
                .thenReturn("$2a$10$storedHash");

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "correct-password");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches(
                "correct-password", "$2a$10$storedHash"))
                .thenReturn(true);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request, "192.0.2.10"));

        verify(jwtService, never()).generateAccessToken(any());
        verifyNoInteractions(refreshTokenService);
        verify(loginAttempt).recordFailure();
    }

    @Test
    void unexpectedLoginFailureReleasesReservationWithoutRecordingFailure() {
        LoginRateLimiter limiter = new LoginRateLimiter(
                1,
                1,
                Duration.ofMinutes(15),
                100,
                Clock.systemUTC());
        AuthService service = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                limiter);
        LoginRequest request = new LoginRequest(
                "test@example.com", "password");
        IllegalStateException unexpected =
                new IllegalStateException("Repository unavailable.");
        when(userRepository.findByEmail("test@example.com"))
                .thenThrow(unexpected);

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> service.login(request, "192.0.2.10"));

        assertSame(unexpected, thrown);
        assertEquals(0, limiter.trackedEmailKeyCount());
        assertEquals(0, limiter.trackedIpKeyCount());
        try (LoginRateLimiter.Attempt subsequent =
                     limiter.beginAttempt(
                             "test@example.com", "192.0.2.10")) {
            subsequent.recordSuccess();
        }
    }

    @Test
    void loginRateLimitRejectsBeforeLookingUpUserOrVerifyingPassword() {
        LoginRequest request = new LoginRequest(
                " TEST@Example.com ",
                "incorrect-password");
        when(loginRateLimiter.beginAttempt(
                "test@example.com", "192.0.2.10"))
                .thenThrow(new LoginRateLimitExceededException(45));

        LoginRateLimitExceededException exception = assertThrows(
                LoginRateLimitExceededException.class,
                () -> authService.login(request, "192.0.2.10"));

        assertEquals(45, exception.getRetryAfterSeconds());
        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService);
    }
}
