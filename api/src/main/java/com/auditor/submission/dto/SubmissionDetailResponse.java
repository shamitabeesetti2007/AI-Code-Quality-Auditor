package com.auditor.submission.dto;

import com.auditor.submission.CodeSubmission;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.SubmissionOriginType;

import java.time.Instant;
import java.util.UUID;

public record SubmissionDetailResponse(
        UUID submissionId,
        String title,
        SubmissionLanguage language,
        SubmissionOriginType originType,
        boolean provenanceVerified,
        String sourceCode,
        String contentHash,
        int logicalLoc,
        Instant createdAt,
        Instant updatedAt) {

    public static SubmissionDetailResponse from(CodeSubmission submission) {
        return new SubmissionDetailResponse(
                submission.getSubmissionId(),
                submission.getTitle(),
                submission.getLanguage(),
                submission.getOriginType(),
                submission.isProvenanceVerified(),
                submission.getSourceCode(),
                submission.getContentHash(),
                submission.getLogicalLoc(),
                submission.getCreatedAt(),
                submission.getUpdatedAt());
    }

    @Override
    public String toString() {
        return "SubmissionDetailResponse[submissionId=" + submissionId
                + ", title=" + title
                + ", contentHash=" + contentHash + "]";
    }
}
