package com.auditor.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityProblemResponseWriterTest {

    @Test
    void committedResponseIsNotModified() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(true);

        new SecurityProblemResponseWriter().write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Authentication is required.");

        verify(response).isCommitted();
        verify(response, never()).setStatus(anyInt());
        verify(response, never()).setContentType(
                anyString());
        verify(response, never()).setHeader(
                anyString(),
                anyString());
        verify(response, never()).getOutputStream();
    }
}
