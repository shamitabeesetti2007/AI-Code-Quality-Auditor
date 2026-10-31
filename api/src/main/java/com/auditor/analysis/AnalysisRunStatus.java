package com.auditor.analysis;

public enum AnalysisRunStatus {
    QUEUED,
    PARSING,
    ANALYZING,
    SCORING,
    COMPLETED,
    PARTIAL,
    PARSE_FAILED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED
                || this == PARTIAL
                || this == PARSE_FAILED
                || this == FAILED;
    }
}
