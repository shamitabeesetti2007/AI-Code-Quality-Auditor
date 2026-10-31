package com.auditor.analysis.contract;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canonical finding data. Severity is null when the producing analyzer does
 * not supply a severity value.
 */
public record AnalyzerFinding(
        String ruleId,
        String fingerprint,
        AnalyzerSeverity severity,
        String message,
        String description,
        String filePath,
        Integer startLine,
        Integer startColumn,
        Integer endLine,
        Integer endColumn,
        String cweId,
        Map<String, Object> metadata) {

    public AnalyzerFinding {
        metadata = Collections.unmodifiableMap(
                new LinkedHashMap<>(metadata == null ? Map.of() : metadata));
    }
}
