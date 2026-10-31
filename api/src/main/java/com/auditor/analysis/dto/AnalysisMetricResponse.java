package com.auditor.analysis.dto;

import com.auditor.analysis.AnalysisMetric;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record AnalysisMetricResponse(
        UUID metricId,
        String dimension,
        String metricKey,
        BigDecimal numericValue,
        Map<String, Object> metadata) {

    public static AnalysisMetricResponse from(AnalysisMetric metric) {
        return new AnalysisMetricResponse(
                metric.getMetricId(),
                metric.getDimension(),
                metric.getMetricKey(),
                metric.getNumericValue(),
                metric.getMetadata());
    }
}
