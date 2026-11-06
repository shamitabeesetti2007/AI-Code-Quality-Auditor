package com.auditor.analysis.engine;

import java.util.List;

public record AnalyzerResult(List<Metric> metrics, List<Finding> findings) {

    public AnalyzerResult {
        metrics = List.copyOf(metrics);
        findings = List.copyOf(findings);
    }

    public static AnalyzerResult empty() {
        return new AnalyzerResult(List.of(), List.of());
    }
}