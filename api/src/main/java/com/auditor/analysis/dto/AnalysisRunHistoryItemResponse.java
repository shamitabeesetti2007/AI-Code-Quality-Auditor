package com.auditor.analysis.dto;

import com.auditor.analysis.AnalysisRunStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnalysisRunHistoryItemResponse(
        UUID runId,
        UUID submissionId,
        AnalysisRunStatus status,
        BigDecimal score,
        String grade,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        String errorSummary) {

    public AnalysisRunHistoryItemResponse withSafeErrorSummary() {
        String safeSummary = AnalysisRunErrorSummary.safeFor(status);
        return new AnalysisRunHistoryItemResponse(
                runId,
                submissionId,
                status,
                score,
                grade,
                createdAt,
                startedAt,
                completedAt,
                safeSummary);
    }
}
