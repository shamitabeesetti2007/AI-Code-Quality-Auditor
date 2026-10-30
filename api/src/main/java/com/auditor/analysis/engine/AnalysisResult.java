package com.auditor.analysis.engine;

import java.util.List;
import java.util.Objects;

public record AnalysisResult(
        AnalysisStatus status,
        List<SyntaxDiagnostic> diagnostics,
        List<Metric> metrics,
        List<Finding> findings) {

    public AnalysisResult {
        Objects.requireNonNull(status, "status must not be null");
        diagnostics = List.copyOf(diagnostics);
        metrics = List.copyOf(metrics);
        findings = List.copyOf(findings);
    }

    public String canonicalRunStatusName() {
        return status == AnalysisStatus.PARSE_FAILED ? "PARSE_FAILED" : "ANALYZED";
    }
}