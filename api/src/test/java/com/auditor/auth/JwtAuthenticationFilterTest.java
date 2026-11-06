package com.auditor.auth;

import com.auditor.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SecurityProblemResponseWriter problemResponseWriter =
            mock(SecurityProblemResponseWriter.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(
                    jwtService,
                    userRepository,
                    problemResponseWriter);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain filterChain = mock(FilterChain.class);

    @Test
    void malformedTokenSubjectIsUnauthorized() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer malformed-subject-token");
        when(jwtService.validateAndGetUserId("malformed-subject-token"))
                .thenThrow(new IllegalArgumentException(
                        "sensitive malformed subject details"));

        filter.doFilterInternal(request, response, filterChain);

        verify(problemResponseWriter).write(
                eq(request),
                eq(response),
                eq(HttpStatus.UNAUTHORIZED),
                eq("Authentication is required or the token is invalid."));
        verifyNoInteractions(userRepository, filterChain);
    }

    @Test
    void jwtClaimValidationFailureIsUnauthorized() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer invalid-claims-token");
        when(jwtService.validateAndGetUserId("invalid-claims-token"))
                .thenThrow(new JwtException("invalid required claim"));

        filter.doFilterInternal(request, response, filterChain);

        verify(problemResponseWriter).write(
                eq(request),
                eq(response),
                eq(HttpStatus.UNAUTHORIZED),
                eq("Authentication is required or the token is invalid."));
        verifyNoInteractions(userRepository, filterChain);
    }

    @Test
    void repositoryIllegalArgumentFailureIsInternalServerError()
            throws Exception {
        UUID userId = UUID.randomUUID();
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");
        when(jwtService.validateAndGetUserId("valid-token"))
                .thenReturn(userId);
        when(userRepository.findById(userId))
                .thenThrow(new IllegalArgumentException(
                        "sensitive repository details"));

        filter.doFilterInternal(request, response, filterChain);

        verify(problemResponseWriter).write(
                eq(request),
                eq(response),
                eq(HttpStatus.INTERNAL_SERVER_ERROR),
                eq("An unexpected authentication error occurred."));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void unauthorizedResponseWriteFailureDoesNotAttemptInternalErrorWrite()
            throws Exception {
        UUID userId = UUID.randomUUID();
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");
        when(jwtService.validateAndGetUserId("valid-token"))
                .thenReturn(userId);
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());
        doThrow(new IllegalStateException("response is unavailable"))
                .when(problemResponseWriter)
                .write(
                        eq(request),
                        eq(response),
                        eq(HttpStatus.UNAUTHORIZED),
                        eq("Authentication is required or the token is invalid."));

        assertThrows(
                IllegalStateException.class,
                () -> filter.doFilterInternal(request, response, filterChain));

        verify(problemResponseWriter).write(
                eq(request),
                eq(response),
                eq(HttpStatus.UNAUTHORIZED),
                eq("Authentication is required or the token is invalid."));
        verifyNoMoreInteractions(problemResponseWriter);
        verify(filterChain, never()).doFilter(request, response);
    }
}
