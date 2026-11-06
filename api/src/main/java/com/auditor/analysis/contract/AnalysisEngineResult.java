package com.auditor.analysis.contract;

import java.util.List;

public record AnalysisEngineResult(
        ParseOutcome parseOutcome,
        String analyzerVersion,
        AnalyzerExecutionStatus executionStatus,
        String errorSummary,
        List<AnalyzerFinding> findings,
        List<AnalyzerMetric> metrics) {

    public AnalysisEngineResult {
        findings = List.copyOf(findings);
        metrics = List.copyOf(metrics);
    }
}
