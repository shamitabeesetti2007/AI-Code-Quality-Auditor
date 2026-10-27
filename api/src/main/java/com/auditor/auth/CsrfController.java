package com.auditor.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/csrf")
public class CsrfController {

    static final String RAW_CSRF_TOKEN_ATTRIBUTE =
            CsrfController.class.getName() + ".rawCsrfToken";

    @GetMapping
    public ResponseEntity<CsrfTokenResponse> getToken(
            CsrfToken csrfToken,
            HttpServletRequest request) {
        CsrfToken rawToken = (CsrfToken) request.getAttribute(
                RAW_CSRF_TOKEN_ATTRIBUTE);
        if (rawToken == null) {
            throw new IllegalStateException(
                    "The CSRF token was not initialized.");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new CsrfTokenResponse(
                        rawToken.getToken(),
                        csrfToken.getHeaderName()));
    }

    public record CsrfTokenResponse(String token, String headerName) {
    }
}
