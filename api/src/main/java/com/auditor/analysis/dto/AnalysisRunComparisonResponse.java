package com.auditor.analysis.dto;

import com.auditor.analysis.AnalysisFinding;
import com.auditor.analysis.AnalysisMetric;
import com.auditor.analysis.AnalysisRunStatus;
import com.auditor.analysis.FindingSeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AnalysisRunComparisonResponse(
        UUID submissionId,
        RunOverview baseline,
        RunOverview target,
        BigDecimal scoreDifference,
        Availability comparisonAvailability,
        MetricsComparison metrics,
        FindingsComparison findings) {

    public enum Availability { COMPLETE, PARTIAL }
    public enum DimensionAvailability { COMPLETE, PARTIAL, UNAVAILABLE }
    public enum Change { ADDED, REMOVED, RETAINED }
    public enum FindingChange { ADDED, RESOLVED, PERSISTING }

    public record RunOverview(UUID runId, AnalysisRunStatus status, BigDecimal score,
            String grade, Instant createdAt, Instant completedAt,
            DimensionAvailability dataAvailability) {
        public RunOverview(UUID runId, AnalysisRunStatus status, BigDecimal score,
                String grade, Instant createdAt, Instant completedAt) {
            this(runId, status, score, grade, createdAt, completedAt, availabilityFor(status));
        }
        private static DimensionAvailability availabilityFor(AnalysisRunStatus status) {
            return switch (status) {
                case COMPLETED -> DimensionAvailability.COMPLETE;
                case PARTIAL -> DimensionAvailability.PARTIAL;
                default -> DimensionAvailability.UNAVAILABLE;
            };
        }
    }

    public record MetricsComparison(DimensionAvailability dataAvailability,
            List<MetricChange> items) {
        public MetricsComparison { items = List.copyOf(items); }
    }

    public record MetricChange(String dimension, String metricKey,
            BigDecimal baselineValue, BigDecimal targetValue, BigDecimal difference,
            Change change) {}

    public record FindingsComparison(DimensionAvailability dataAvailability,
            List<FindingChangeItem> items, Map<FindingChange, Long> counts,
            Map<FindingChange, Map<FindingSeverity, Long>> countsBySeverity,
            Map<FindingChange, Long> unclassifiedSeverityCounts) {
        public FindingsComparison {
            items = List.copyOf(items);
            if (counts != null) counts = Collections.unmodifiableMap(new EnumMap<>(counts));
            if (countsBySeverity != null) {
                EnumMap<FindingChange, Map<FindingSeverity, Long>> copied =
                        new EnumMap<>(FindingChange.class);
                countsBySeverity.forEach((key, value) -> {
                    EnumMap<FindingSeverity, Long> severities = new EnumMap<>(FindingSeverity.class);
                    severities.putAll(value);
                    copied.put(key, Collections.unmodifiableMap(severities));
                });
                countsBySeverity = Collections.unmodifiableMap(copied);
            }
            if (unclassifiedSeverityCounts != null) {
                unclassifiedSeverityCounts = Collections.unmodifiableMap(
                        new EnumMap<>(unclassifiedSeverityCounts));
            }
        }
    }

    public record FindingChangeItem(FindingChange change, String ruleId, String fingerprint,
            String message, FindingSeverity severity, String filePath, Integer startLine,
            Integer startColumn, Integer endLine, Integer endColumn) {
        public static FindingChangeItem of(FindingChange change, AnalysisFinding finding) {
            return new FindingChangeItem(change, finding.getRuleId(), finding.getFingerprint(),
                    finding.getMessage(), finding.getSeverity(), finding.getFilePath(),
                    finding.getStartLine(), finding.getStartColumn(), finding.getEndLine(),
                    finding.getEndColumn());
        }
    }
}
