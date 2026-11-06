package com.auditor.analysis.dto;

import com.auditor.analysis.AnalysisRunStatus;

public final class AnalysisRunErrorSummary {
    private AnalysisRunErrorSummary() {}

    public static String safeFor(AnalysisRunStatus status) {
        return switch (status) {
            case FAILED -> "Analysis failed unexpectedly.";
            case PARSE_FAILED -> "The Java source could not be parsed.";
            case PARTIAL -> "Analysis completed with unavailable checks.";
            default -> null;
        };
    }
}
