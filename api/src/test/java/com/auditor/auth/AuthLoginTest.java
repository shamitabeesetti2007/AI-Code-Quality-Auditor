
package com.auditor.auth;

import com.auditor.auth.dto.LoginRequest;
import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

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
    void loginReturnsTokenForValidCredentials() {
        LoginRequest request = new LoginRequest(
                " TEST@Example.com ",
                "correct-password");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(
                "correct-password", activeUser.getPasswordHash()))
                .thenReturn(true);
        when(jwtService.generateAccessToken(activeUser))
                .thenReturn("test-access-token");

        var response = authService.login(request);

        assertEquals("test-access-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertEquals("test@example.com", response.user().email());

        verify(jwtService).generateAccessToken(activeUser);
    }

    @Test
    void loginRejectsIncorrectPassword() {
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
                () -> authService.login(request));

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void loginRejectsUnknownEmail() {
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
                () -> authService.login(request));

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void loginRejectsDisabledAccount() {
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
                () -> authService.login(request));

        verify(jwtService, never()).generateAccessToken(any());
    }
}

