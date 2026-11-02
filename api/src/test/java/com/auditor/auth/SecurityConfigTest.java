package com.auditor.auth;

import com.auditor.auth.dto.LoginResponse;
import com.auditor.auth.dto.UserResponse;
import com.auditor.auth.token.InvalidRefreshTokenException;
import com.auditor.auth.token.RefreshTokenService;
import com.auditor.analysis.AnalysisRunController;
import com.auditor.analysis.AnalysisRunService;
import com.auditor.submission.SubmissionController;
import com.auditor.submission.SubmissionService;
import com.auditor.submission.SubmissionOriginType;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.dto.CreateSubmissionRequest;
import com.auditor.submission.dto.PageResponse;
import com.auditor.submission.dto.SubmissionDetailResponse;
import com.auditor.submission.dto.SubmissionResponse;
import com.auditor.user.AdminUserController;
import com.auditor.user.AppUser;
import com.auditor.user.RoleManagementService;
import com.auditor.user.UserController;
import com.auditor.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        CsrfController.class,
        UserController.class,
        AdminUserController.class,
        SubmissionController.class,
        AnalysisRunController.class
}, properties = "app.security.allowed-origins=http://localhost:5173")
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SecurityProblemResponseWriter.class,
        AuthConfiguration.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleManagementService roleManagementService;

    @MockitoBean
    private SubmissionService submissionService;

    @MockitoBean
    private AnalysisRunService analysisRunService;

    @Test
    void csrfEndpointProvisionsCookieAndHeaderToken() throws Exception {
        mockMvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        allOf(
                                containsString("XSRF-TOKEN="),
                                org.hamcrest.Matchers.not(
                                        containsString("HttpOnly")))))
                .andExpect(content().json("""
                        {
                          "headerName": "X-XSRF-TOKEN"
                        }
                        """))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void corsPreflightAllowsConfiguredCredentialedApiRequest()
            throws Exception {
        mockMvc.perform(options("/api/v1/auth/refresh")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header(
                                "Access-Control-Request-Headers",
                                "authorization,content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:5173"))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string(
                        "Access-Control-Allow-Methods",
                        containsString("POST")))
                .andExpect(header().string(
                        "Access-Control-Allow-Headers",
                        allOf(
                                containsString("authorization"),
                                containsString("content-type"),
                                containsString("x-xsrf-token"))));
    }

    @Test
    void corsRejectsUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/refresh")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(
                        "Access-Control-Allow-Origin"));
    }

    @Test
    void corsRejectsWildcardOriginsWithCredentials() {
        SecurityConfig securityConfig =
                new SecurityConfig(null, null);

        assertThrows(
                IllegalStateException.class,
                () -> securityConfig.corsConfigurationSource("*"));
    }

    @Test
    void currentUserWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "type": "about:blank",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "Authentication is required.",
                          "instance": "/api/v1/users/me"
                        }
                        """));
    }

    @Test
    void currentUserWithInvalidTokenIsUnauthorized() throws Exception {
        when(jwtService.validateAndGetUserId(any()))
                .thenThrow(
                        new io.jsonwebtoken.MalformedJwtException(
                                "sensitive JWT parser details"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "type": "about:blank",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "Authentication is required or the token is invalid.",
                          "instance": "/api/v1/users/me"
                        }
                        """))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(containsString(
                                "sensitive JWT parser details"))));
    }

    @Test
    void authenticatedRequestToUnlistedEndpointIsForbidden()
            throws Exception {
        mockMvc.perform(get("/api/v1/not-allowed")
                        .with(user("test-user")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "type": "about:blank",
                          "title": "Forbidden",
                          "status": 403,
                          "detail": "Access is denied.",
                          "instance": "/api/v1/not-allowed"
                          }
                          """));
    }

    @Test
    void submissionAndAnalysisEndpointsRequireAuthentication()
            throws Exception {
        mockMvc.perform(get("/api/v1/submissions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(
                        "/api/v1/analysis-runs/{runId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submissionWritesRemainProtectedByCsrf() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/submissions/{submissionId}/analysis-runs",
                        UUID.randomUUID())
                        .with(user(UUID.randomUUID().toString())))
                .andExpect(status().isForbidden());

        verify(analysisRunService, never())
                .createQueuedRun(any(), any());
    }

    @Test
    void corsAllowsSubmissionPutAndDeleteMethods() throws Exception {
        mockMvc.perform(options("/api/v1/submissions/" + UUID.randomUUID())
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Methods",
                        containsString("PUT")));
        mockMvc.perform(options("/api/v1/submissions/" + UUID.randomUUID())
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Methods",
                        containsString("DELETE")));
    }

    @Test
    void createUsesPrincipalIdentityAndDoesNotReturnSubmittedSource()
            throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        var response = new SubmissionResponse(
                submissionId,
                "Example",
                SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN,
                false,
                "a".repeat(64),
                1,
                Instant.now(),
                Instant.now());
        when(submissionService.create(
                eq(ownerId), any(CreateSubmissionRequest.class)))
                .thenReturn(new SubmissionService.CreatedSubmission(
                        response, null));
        CsrfTestToken csrfToken = provisionCsrfToken();

        mockMvc.perform(post("/api/v1/submissions/")
                        .with(user(ownerId.toString()))
                        .cookie(csrfToken.cookie())
                        .header("X-XSRF-TOKEN", csrfToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Example",
                                  "language": "JAVA",
                                  "originType": "HUMAN",
                                  "sourceCode": "class Example {}",
                                  "autoAnalyze": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "/api/v1/submissions/" + submissionId))
                .andExpect(jsonPath("$.submission.sourceCode").doesNotExist());

        verify(submissionService).create(
                eq(ownerId), any(CreateSubmissionRequest.class));
    }

    @Test
    void listOmitsSourceWhileDetailReturnsItWithoutCaching() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Instant now = Instant.now();
        SubmissionResponse listItem = new SubmissionResponse(
                submissionId,
                "Example",
                SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN,
                false,
                "a".repeat(64),
                1,
                now,
                now);
        when(submissionService.list(ownerId, 0, 20))
                .thenReturn(new PageResponse<>(
                        List.of(listItem), 0, 20, 1, 1));
        when(submissionService.get(ownerId, submissionId))
                .thenReturn(new SubmissionDetailResponse(
                        submissionId,
                        "Example",
                        SubmissionLanguage.JAVA,
                        SubmissionOriginType.HUMAN,
                        false,
                        "class Example {}",
                        "a".repeat(64),
                        1,
                        now,
                        now));

        mockMvc.perform(get("/api/v1/submissions")
                        .with(user(ownerId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].submissionId")
                        .value(submissionId.toString()))
                .andExpect(jsonPath("$.items[0].sourceCode").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(get("/api/v1/submissions/{id}", submissionId)
                        .with(user(ownerId.toString())))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.sourceCode").value("class Example {}"));

        verify(submissionService).list(ownerId, 0, 20);
        verify(submissionService).get(ownerId, submissionId);
    }

    @Test
    void roleChangeRequiresAdministrator() throws Exception {
        CsrfTestToken csrfToken = provisionCsrfToken();
        mockMvc.perform(patch(
                                "/api/v1/admin/users/{userId}/role",
                                UUID.randomUUID())
                        .cookie(csrfToken.cookie())
                        .header("X-XSRF-TOKEN", csrfToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"RESEARCHER\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch(
                                "/api/v1/admin/users/{userId}/role",
                                UUID.randomUUID())
                        .with(user("00000000-0000-0000-0000-000000000001")
                                .roles("USER"))
                        .cookie(csrfToken.cookie())
                        .header("X-XSRF-TOKEN", csrfToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"RESEARCHER\"}"))
                .andExpect(status().isForbidden());

        verify(roleManagementService, never()).changeRole(
                any(), any(), any());
    }

    @Test
    void administratorCanRequestRoleChange() throws Exception {
        CsrfTestToken csrfToken = provisionCsrfToken();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(roleManagementService.changeRole(
                actorId,
                targetId,
                AppUser.Role.RESEARCHER))
                .thenReturn(new UserResponse(
                        targetId,
                        "user@example.com",
                        "Test User",
                        "RESEARCHER"));

        mockMvc.perform(patch(
                                "/api/v1/admin/users/{userId}/role",
                                targetId)
                        .with(user(actorId.toString()).roles("ADMIN"))
                        .cookie(csrfToken.cookie())
                        .header("X-XSRF-TOKEN", csrfToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"RESEARCHER\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "id": "%s",
                          "email": "user@example.com",
                          "displayName": "Test User",
                          "role": "RESEARCHER"
                        }
                        """.formatted(targetId)));

        verify(roleManagementService).changeRole(
                actorId,
                targetId,
                AppUser.Role.RESEARCHER);
    }

    @Test
    void administratorRoleChangeRequiresProvisionedCsrfToken()
            throws Exception {
        mockMvc.perform(patch(
                                "/api/v1/admin/users/{userId}/role",
                                UUID.randomUUID())
                        .with(user(UUID.randomUUID().toString())
                                .roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"RESEARCHER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON));

        verify(roleManagementService, never()).changeRole(
                any(), any(), any());
    }

    @Test
    void jwtFilterInternalFailureReturnsGenericProblem() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.validateAndGetUserId("test-token"))
                        .thenReturn(userId);
        when(userRepository.findById(userId))
                        .thenThrow(new IllegalArgumentException(
                                "sensitive database detail"));

        mockMvc.perform(get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer test-token"))
                        .andExpect(status().isInternalServerError())
                        .andExpect(content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(content().json("""
                                {
                                  "type": "about:blank",
                                  "title": "Internal Server Error",
                                  "status": 500,
                                  "detail": "An unexpected authentication error occurred.",
                                  "instance": "/api/v1/users/me"
                                }
                                """))
                        .andExpect(content().string(
                                org.hamcrest.Matchers.not(containsString(
                                        "sensitive database detail"))));
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
        UserResponse user = new UserResponse(
                UUID.randomUUID(),
                "test@example.com",
                "Test User",
                "USER");

        when(authService.login(any(), any()))
                .thenReturn(new AuthResult(
                        LoginResponse.from("test-access-token", user),
                        "test-refresh-token",
                        Instant.now().plusSeconds(7L * 24 * 60 * 60)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(
                        HttpHeaders.PRAGMA, "no-cache"));
    }

    @Test
    void loginUsesDirectPeerIpInsteadOfForwardedHeader() throws Exception {
        UserResponse user = new UserResponse(
                UUID.randomUUID(),
                "test@example.com",
                "Test User",
                "USER");
        when(authService.login(any(), any()))
                .thenReturn(new AuthResult(
                        LoginResponse.from("test-access-token", user),
                        "test-refresh-token",
                        Instant.now().plusSeconds(7L * 24 * 60 * 60)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        })
                        .header("X-Forwarded-For", "198.51.100.25")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isOk());

        verify(authService).login(any(), eq("192.0.2.10"));
    }

    @Test
    void loginRateLimitReturnsGenericProblemAndRetryAfter()
            throws Exception {
        when(authService.login(any(), any()))
                .thenThrow(new LoginRateLimitExceededException(37));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "private-user@example.com",
                                  "password": "incorrect-password"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "37"))
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "status": 429,
                          "detail": "Too many login attempts. Please try again later."
                        }
                        """))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                containsString("private-user@example.com"))));
    }

    @Test
    void loginCredentialFailuresUseGenericUnauthorizedProblem()
            throws Exception {
        when(authService.login(any(), any()))
                .thenThrow(new InvalidCredentialsException());

        for (String email : new String[] {
                "unknown@example.com", "known@example.com"}) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "%s",
                                      "password": "incorrect-password"
                                    }
                                    """.formatted(email)))
                    .andExpect(status().isUnauthorized())
                                    .andExpect(header().string(
                                            HttpHeaders.CACHE_CONTROL, "no-store"))
                                    .andExpect(content().contentTypeCompatibleWith(
                                            MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(content().json("""
                            {
                              "status": 401,
                              "detail": "Invalid email or password."
                            }
                            """))
                    .andExpect(content().string(
                            org.hamcrest.Matchers.not(
                                    containsString(email))));
        }
    }

    @Test
    void refreshWithoutOriginIsForbidden() throws Exception {
        CsrfTestToken csrfToken = provisionCsrfToken();
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrfToken.cookie())
                        .header("X-XSRF-TOKEN", csrfToken.value()))
                .andExpect(status().isForbidden());

        verify(refreshTokenService, never()).rotate(any());
    }

    @Test
    void refreshWithoutCsrfProtectionHeaderIsForbidden()
            throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isForbidden());

        verify(refreshTokenService, never()).rotate(any());
    }

    @Test
    void refreshFromUntrustedOriginIsForbidden() throws Exception {
        CsrfTestToken csrfToken = provisionCsrfToken();
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrfToken.cookie())
                        .header("Origin", "https://untrusted.example")
                        .header("X-XSRF-TOKEN", csrfToken.value()))
                .andExpect(status().isForbidden());

        verify(refreshTokenService, never()).rotate(any());
    }

    @Test
    void logoutWithoutOriginIsForbidden() throws Exception {
        CsrfTestToken csrfToken = provisionCsrfToken();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(csrfToken.cookie())
                        .header("X-XSRF-TOKEN", csrfToken.value()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "status": 403,
                          "detail": "Request rejected by security policy."
                        }
                        """))
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"));

        verify(refreshTokenService, never()).logout(any());
    }

    @Test
    void logoutWithInvalidCsrfHeaderIsForbidden() throws Exception {
        CsrfTestToken csrfToken = provisionCsrfToken();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(csrfToken.cookie())
                        .header("Origin", "http://localhost:5173")
                        .header("X-XSRF-TOKEN", "invalid"))
                .andExpect(status().isForbidden());

        verify(refreshTokenService, never()).logout(any());
    }

    @Test
    void refreshWithInvalidTokenReturnsUnauthorizedAndClearsCookie()
            throws Exception {
        CsrfTestToken csrfToken = provisionCsrfToken();
        when(refreshTokenService.rotate("invalid-token"))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrfToken.cookie())
                        .cookie(new Cookie(
                                "refresh_token", "invalid-token"))
                        .header("Origin", "http://localhost:5173")
                        .header("X-XSRF-TOKEN", csrfToken.value()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        allOf(
                                containsString("refresh_token="),
                                containsString("HttpOnly"),
                                containsString("SameSite=Strict"),
                                containsString("Path=/api/v1/auth"),
                                containsString("Max-Age=0"))))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON));
    }
    @Test
    void logoutSucceedsWithoutCookieAndClearsCookie() throws Exception {
        CsrfTestToken csrfToken = provisionCsrfToken();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(csrfToken.cookie())
                        .header("Origin", "http://localhost:5173")
                        .header("X-XSRF-TOKEN", csrfToken.value()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        allOf(
                                containsString("refresh_token="),
                                containsString("HttpOnly"),
                                containsString("SameSite=Strict"),
                                containsString("Path=/api/v1/auth"),
                                containsString("Max-Age=0"))));

        verify(refreshTokenService).logout(null);
    }

    @Test
    void logoutRevocationFailureReturnsFailureAndClearsCookie()
            throws Exception {
        CsrfTestToken csrfToken = provisionCsrfToken();
        doThrow(new IllegalStateException("sensitive revocation details"))
                .when(refreshTokenService).logout("refresh-value");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(csrfToken.cookie())
                        .cookie(new Cookie("refresh_token", "refresh-value"))
                        .header("Origin", "http://localhost:5173")
                        .header("X-XSRF-TOKEN", csrfToken.value()))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        allOf(
                                containsString("refresh_token="),
                                containsString("HttpOnly"),
                                containsString("Secure"),
                                containsString("SameSite=Strict"),
                                containsString("Path=/api/v1/auth"),
                                containsString("Max-Age=0"))))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "type": "about:blank",
                          "title": "Internal Server Error",
                          "status": 500,
                          "detail": "An unexpected error occurred."
                        }
                        """))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                containsString("sensitive revocation details"))));
    }

    private CsrfTestToken provisionCsrfToken() throws Exception {
        var result = mockMvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        String token = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.token");
        assertEquals(cookie.getValue(), token);
        return new CsrfTestToken(cookie, token);
    }

    private record CsrfTestToken(Cookie cookie, String value) {
    }
}
