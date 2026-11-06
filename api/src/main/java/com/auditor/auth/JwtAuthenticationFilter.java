
package com.auditor.auth;

import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityProblemResponseWriter problemResponseWriter;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            SecurityProblemResponseWriter problemResponseWriter) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.problemResponseWriter = problemResponseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);
        UUID userId;

        try {
            userId = jwtService.validateAndGetUserId(token);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(request, response);
            return;
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            writeInternalServerError(request, response);
            return;
        }

        AppUser user;
        boolean active;
        try {
            user = userRepository.findById(userId).orElse(null);
            active = user != null
                    && user.getAccountStatus() == AppUser.AccountStatus.ACTIVE;
            if (active) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        user.getUserId().toString(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(
                        authentication);
            }
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            writeInternalServerError(request, response);
            return;
        }

        if (!active) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeInternalServerError(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {
        problemResponseWriter.write(
                request,
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected authentication error occurred.");
    }

    private void writeUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {
        problemResponseWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Authentication is required or the token is invalid.");
    }
}
