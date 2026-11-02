package com.auditor.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FailingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void unexpectedControllerFailureReturnsGenericProblem() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Internal Server Error",
                          "status": 500,
                          "detail": "An unexpected error occurred."
                        }
                        """))
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().string(
                        not(containsString("sensitive service detail"))));
    }

    @Test
    void illegalArgumentReturnsSanitizedClientError() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Request contains invalid values."
                        }
                        """))
                .andExpect(content().string(
                        not(containsString("sensitive implementation detail"))));
    }

    @Test
    void duplicateActiveAnalysisReturnsConflictProblem() throws Exception {
        mockMvc.perform(get("/test/active-run-conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Conflict",
                          "status": 409,
                          "detail": "An analysis run is already active for this submission."
                        }
                        """));
    }

    @Test
    void unreadableRequestReturnsSanitizedProblem() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "status": 400,
                          "detail": "Request contains invalid values."
                        }
                        """));
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/unexpected")
        String unexpectedFailure() {
            throw new IllegalStateException("sensitive service detail");
        }

        @GetMapping("/test/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException(
                    "sensitive implementation detail");
        }

        @GetMapping("/test/active-run-conflict")
        String activeRunConflict() {
            throw new DataIntegrityViolationException(
                    "duplicate",
                    new org.hibernate.exception.ConstraintViolationException(
                            "duplicate",
                            new java.sql.SQLException("duplicate"),
                            "uq_analysis_run_one_active_per_submission"));
        }

        @PostMapping("/test/body")
        String body(@RequestBody TestRequest request) {
            return request.value();
        }
    }

    record TestRequest(String value) {
    }
}
