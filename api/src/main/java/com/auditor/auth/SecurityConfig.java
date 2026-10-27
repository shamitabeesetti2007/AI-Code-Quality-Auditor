
package com.auditor.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityProblemResponseWriter problemResponseWriter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecurityProblemResponseWriter problemResponseWriter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.problemResponseWriter = problemResponseWriter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        return http
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                csrfTokenRepository())
                        .csrfTokenRequestHandler(
                                new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(
                                request -> HttpMethod.POST.matches(
                                        request.getMethod())
                                        && isPublicAuthPost(request)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/csrf").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/me").authenticated()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.allowed-origins:http://localhost:5173}")
            String configuredOrigins) {
        List<String> allowedOrigins = Arrays.stream(
                        configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (allowedOrigins.contains("*")) {
            throw new IllegalStateException(
                    "Credentialed CORS requires explicit allowed origins.");
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) ->
                problemResponseWriter.write(
                        request,
                        response,
                        HttpStatus.UNAUTHORIZED,
                        "Authentication is required.");
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) ->
                problemResponseWriter.write(
                        request,
                        response,
                        HttpStatus.FORBIDDEN,
                        "Access is denied.");
    }

    private static boolean isPublicAuthPost(HttpServletRequest request) {
        String path = request.getRequestURI().substring(
                request.getContextPath().length());
        return "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/login".equals(path);
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie ->
                cookie.sameSite("Strict"));
        return repository;
    }

    private static final class SpaCsrfTokenRequestHandler
            implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler xorHandler =
                new XorCsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler plainHandler =
                new CsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {
            xorHandler.handle(request, response, csrfToken);
            CsrfToken token = csrfToken.get();
            token.getToken();
            request.setAttribute(
                    CsrfController.RAW_CSRF_TOKEN_ATTRIBUTE, token);
        }

        @Override
        public String resolveCsrfTokenValue(
                HttpServletRequest request,
                CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return StringUtils.hasText(headerValue)
                    ? plainHandler.resolveCsrfTokenValue(request, csrfToken)
                    : xorHandler.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
