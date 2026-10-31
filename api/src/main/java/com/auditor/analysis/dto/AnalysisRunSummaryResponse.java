package com.auditor.analysis.dto;

import com.auditor.analysis.AnalysisRun;
import com.auditor.analysis.AnalysisRunStatus;

import java.time.Instant;
import java.util.UUID;

public record AnalysisRunSummaryResponse(
        UUID runId,
        UUID submissionId,
        String sourceHash,
        AnalysisRunStatus status,
        String analyzerVersion,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        String errorSummary) {

    public static AnalysisRunSummaryResponse from(AnalysisRun run) {
        return new AnalysisRunSummaryResponse(
                run.getRunId(),
                run.getSubmissionId(),
                run.getSourceHash(),
                run.getStatus(),
                run.getAnalyzerVersion(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getCreatedAt(),
                run.getErrorSummary());
    }
}
