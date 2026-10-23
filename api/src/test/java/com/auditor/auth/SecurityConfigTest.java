package com.auditor.auth;

import com.auditor.auth.dto.LoginResponse;
import com.auditor.auth.dto.UserResponse;
import com.auditor.user.UserController;
import com.auditor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        UserController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        AuthConfiguration.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void currentUserWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserWithInvalidTokenIsUnauthorized() throws Exception {
        when(jwtService.validateAndGetUserId(any()))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException("Invalid token"));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequestToUnlistedEndpointIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/not-allowed")
                        .with(user("test-user"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationEndpointIsPublic() throws Exception {
        when(authService.register(any()))
                .thenReturn(new UserResponse(
                        UUID.randomUUID(),
                        "test@example.com",
                        "Test User",
                        "USER"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "StrongPassword123!",
                                  "displayName": "Test User"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void loginEndpointIsPublic() throws Exception {
        when(authService.login(any()))
                .thenReturn(LoginResponse.from(
                        "test-access-token",
                        new UserResponse(
                                UUID.randomUUID(),
                                "test@example.com",
                                "Test User",
                                "USER")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isOk());
    }
}