package com.auditor.analysis.dto;

import com.auditor.analysis.AnalysisRunStatus;
import com.auditor.analysis.FindingSeverity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AnalysisReportResponse(
        AnalysisReportRunOverview run,
        DataAvailability dataAvailability,
        Map<String, List<AnalysisMetricResponse>> metricsByDimension,
        FindingsSummary findingsSummary,
        List<AnalysisFindingResponse> findings) {

    public AnalysisReportResponse {
        Map<String, List<AnalysisMetricResponse>> copiedMetrics =
                new LinkedHashMap<>();
        metricsByDimension.forEach(
                (dimension, metrics) -> copiedMetrics.put(dimension, List.copyOf(metrics)));
        metricsByDimension = Collections.unmodifiableMap(copiedMetrics);
        findings = List.copyOf(findings);
    }

    public static DataAvailability availabilityFor(AnalysisRunStatus status) {
        return switch (status) {
            case COMPLETED -> DataAvailability.COMPLETE;
            case PARTIAL -> DataAvailability.PARTIAL;
            default -> DataAvailability.UNAVAILABLE;
        };
    }

    public enum DataAvailability {
        COMPLETE,
        PARTIAL,
        UNAVAILABLE
    }

    public record FindingsSummary(
            long totalCount,
            Map<FindingSeverity, Long> countsBySeverity,
            long unclassifiedSeverityCount) {

        public FindingsSummary {
            EnumMap<FindingSeverity, Long> copiedCounts =
                    new EnumMap<>(FindingSeverity.class);
            for (FindingSeverity severity : FindingSeverity.values()) {
                copiedCounts.put(severity, countsBySeverity.getOrDefault(severity, 0L));
            }
            countsBySeverity = Collections.unmodifiableMap(copiedCounts);
        }
    }
}
