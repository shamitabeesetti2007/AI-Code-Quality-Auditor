package com.auditor.submission;

import com.auditor.common.AuthenticatedUserId;
import com.auditor.submission.dto.CreateSubmissionRequest;
import com.auditor.submission.dto.PageResponse;
import com.auditor.submission.dto.SubmissionDetailResponse;
import com.auditor.submission.dto.SubmissionResponse;
import com.auditor.submission.dto.UpdateSubmissionRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<SubmissionService.CreatedSubmission> create(
            Authentication authentication,
            @Valid @RequestBody CreateSubmissionRequest request) {
        SubmissionService.CreatedSubmission created =
                submissionService.create(
                        AuthenticatedUserId.from(authentication), request);
        return ResponseEntity.created(URI.create(
                        "/api/v1/submissions/"
                                + created.submission().submissionId()))
                .body(created);
    }

    @GetMapping({"", "/"})
    public PageResponse<SubmissionResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return submissionService.list(
                AuthenticatedUserId.from(authentication), page, size);
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<SubmissionDetailResponse> get(
            Authentication authentication,
            @PathVariable UUID submissionId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(submissionService.get(
                        AuthenticatedUserId.from(authentication),
                        submissionId));
    }

    @PutMapping("/{submissionId}")
    public SubmissionResponse update(
            Authentication authentication,
            @PathVariable UUID submissionId,
            @Valid @RequestBody UpdateSubmissionRequest request) {
        return submissionService.update(
                AuthenticatedUserId.from(authentication),
                submissionId,
                request);
    }

    @DeleteMapping("/{submissionId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable UUID submissionId) {
        submissionService.delete(
                AuthenticatedUserId.from(authentication), submissionId);
        return ResponseEntity.noContent().build();
    }
}
