
package com.auditor.auth;

import com.auditor.auth.dto.RegisterRequest;
import com.auditor.common.EmailAlreadyRegisteredException;
import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        RegisterRequest request = new RegisterRequest(
                "  TEST@Example.com ",
                "a-strong-password",
                " Test User ");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("a-strong-password"))
                .thenReturn("$2a$hashed-value");

        when(userRepository.saveAndFlush(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(request);

        ArgumentCaptor<AppUser> captor =
                ArgumentCaptor.forClass(AppUser.class);

        verify(userRepository).saveAndFlush(captor.capture());

        AppUser savedUser = captor.getValue();

        assertEquals("test@example.com", savedUser.getEmail());
        assertNotEquals("a-strong-password", savedUser.getPasswordHash());
        assertEquals("$2a$hashed-value", savedUser.getPasswordHash());
        assertNotNull(response);

        verify(passwordEncoder).encode("a-strong-password");
    }

    @Test
    void registerRejectsExistingEmail() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "a-strong-password",
                "Test User");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> authService.register(request));

        verify(userRepository, never()).saveAndFlush(any(AppUser.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerRejectsPasswordOverBcryptByteLimit() {
        String longPassword = "é".repeat(37);

        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                longPassword,
                "Test User");

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void registerRejectsDisplayNameThatBecomesEmptyAfterTrimming() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "a-strong-password",
                "   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void registerTranslatesEmailUniquenessRace() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "a-strong-password",
                "Test User");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("a-strong-password"))
                .thenReturn("$2a$hashed-value");

        ConstraintViolationException databaseException =
                new ConstraintViolationException(
                        "Duplicate email",
                        new SQLException("Duplicate email"),
                        "insert into app_user",
                        "app_user_email_key");

        when(userRepository.saveAndFlush(any(AppUser.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Database constraint violation",
                        databaseException));

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> authService.register(request));
    }

    @Test
    void registerRethrowsUnrelatedDatabaseConstraintViolation() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "a-strong-password",
                "Test User");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("a-strong-password"))
                .thenReturn("$2a$hashed-value");

        ConstraintViolationException databaseException =
                new ConstraintViolationException(
                        "Unrelated constraint violation",
                        new SQLException("Constraint violation"),
                        "insert into app_user",
                        "some_other_constraint");

        DataIntegrityViolationException databaseError =
                new DataIntegrityViolationException(
                        "Database constraint violation",
                        databaseException);

        when(userRepository.saveAndFlush(any(AppUser.class)))
                .thenThrow(databaseError);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> authService.register(request));

        assertSame(databaseError, thrown);
    }
}
