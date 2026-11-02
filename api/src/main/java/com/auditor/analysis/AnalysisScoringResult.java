package com.auditor.analysis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AnalysisScoringResult(
        Availability availability,
        String version,
        BigDecimal score,
        String grade,
        String explanation,
        Map<String, BigDecimal> deductions) {

    public AnalysisScoringResult {
        deductions = Collections.unmodifiableMap(
                new LinkedHashMap<>(deductions == null ? Map.of() : deductions));
    }

    public enum Availability {
        AVAILABLE,
        INSUFFICIENT_DATA,
        INCOMPLETE_ANALYSIS
    }
}
