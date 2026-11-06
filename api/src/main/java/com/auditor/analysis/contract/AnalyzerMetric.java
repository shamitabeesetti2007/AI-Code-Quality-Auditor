package com.auditor.analysis.contract;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AnalyzerMetric(
        String dimension,
        String key,
        BigDecimal numericValue,
        Map<String, Object> metadata) {

    public AnalyzerMetric {
        metadata = Collections.unmodifiableMap(
                new LinkedHashMap<>(metadata == null ? Map.of() : metadata));
    }
}
