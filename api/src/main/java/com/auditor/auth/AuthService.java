
package com.auditor.auth;

import com.auditor.auth.dto.LoginRequest;
import com.auditor.auth.dto.LoginResponse;
import com.auditor.auth.dto.RegisterRequest;
import com.auditor.auth.dto.UserResponse;
import com.auditor.common.EmailAlreadyRegisteredException;
import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final String EMAIL_UNIQUE_CONSTRAINT =
            "app_user_email_key";

    // Valid BCrypt hash used to perform a password check for unknown emails.
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String displayName = request.displayName().trim();
        String password = request.password();

        if (displayName.isEmpty()) {
            throw new IllegalArgumentException(
                    "Display name must not be blank.");
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Password exceeds the supported length.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = passwordEncoder.encode(password);
        AppUser user = new AppUser(email, passwordHash, displayName);

        try {
            AppUser saved = userRepository.saveAndFlush(user);
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isEmailUniqueViolation(ex)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<AppUser> candidate = userRepository.findByEmail(email);

        if (candidate.isEmpty()) {
            passwordEncoder.matches(
                    request.password(), DUMMY_PASSWORD_HASH);
            throw new InvalidCredentialsException();
        }

        AppUser user = candidate.get();

        boolean passwordMatches = passwordEncoder.matches(
                request.password(), user.getPasswordHash());

        if (!passwordMatches
                || user.getAccountStatus() != AppUser.AccountStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateAccessToken(user);

        return LoginResponse.from(token, UserResponse.from(user));
    }

    private boolean isEmailUniqueViolation(
            DataIntegrityViolationException ex) {
        Throwable cause = ex;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintEx
                    && EMAIL_UNIQUE_CONSTRAINT.equals(
                            constraintEx.getConstraintName())) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}

