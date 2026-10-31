package com.auditor.submission.dto;

import com.auditor.submission.CodeSubmission;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.SubmissionOriginType;

import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        UUID submissionId,
        String title,
        SubmissionLanguage language,
        SubmissionOriginType originType,
        boolean provenanceVerified,
        String contentHash,
        int logicalLoc,
        Instant createdAt,
        Instant updatedAt) {

    public static SubmissionResponse from(CodeSubmission submission) {
        return new SubmissionResponse(
                submission.getSubmissionId(),
                submission.getTitle(),
                submission.getLanguage(),
                submission.getOriginType(),
                submission.isProvenanceVerified(),
                submission.getContentHash(),
                submission.getLogicalLoc(),
                submission.getCreatedAt(),
                submission.getUpdatedAt());
    }
}
