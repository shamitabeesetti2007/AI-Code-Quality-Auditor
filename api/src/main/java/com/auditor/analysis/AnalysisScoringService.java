package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.AnalyzerFinding;
import com.auditor.analysis.contract.AnalyzerMetric;
import com.auditor.analysis.contract.ParseOutcome;
import com.auditor.analysis.engine.ComplexityAnalyzer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalysisScoringService {

    private static final String CYCLOMATIC_RULE =
            ComplexityAnalyzer.CYCLOMATIC_RULE_ID;
    private static final String COGNITIVE_RULE =
            ComplexityAnalyzer.COGNITIVE_RULE_ID;
    private static final String CYCLOMATIC_METRIC =
            ComplexityAnalyzer.CYCLOMATIC_METRIC_KEY;
    private static final String COGNITIVE_METRIC =
            ComplexityAnalyzer.COGNITIVE_METRIC_KEY;
    private static final String COMPLEXITY_DIMENSION =
            ComplexityAnalyzer.ANALYZER_ID;
    private static final BigDecimal MAXIMUM_SCORE = BigDecimal.valueOf(100);

    private final AnalysisScoringPolicy policy;

    public AnalysisScoringService(AnalysisScoringPolicy policy) {
        this.policy = policy;
    }

    public AnalysisScoringResult score(AnalysisEngineResult result) {
        policy.validate();
        if (result.parseOutcome() != ParseOutcome.SUCCEEDED
                || result.executionStatus() != AnalyzerExecutionStatus.COMPLETED) {
            return unavailable(
                    AnalysisScoringResult.Availability.INCOMPLETE_ANALYSIS,
                    "Only fully parsed, completed analysis can be scored.");
        }

        List<AnalyzerMetric> cyclomaticMetrics = new ArrayList<>();
        List<AnalyzerMetric> cognitiveMetrics = new ArrayList<>();
        for (AnalyzerMetric metric : result.metrics()) {
            if (!COMPLEXITY_DIMENSION.equals(metric.dimension())) {
                continue;
            }
            if (CYCLOMATIC_METRIC.equals(metric.key())) {
                cyclomaticMetrics.add(metric);
            } else if (COGNITIVE_METRIC.equals(metric.key())) {
                cognitiveMetrics.add(metric);
            }
        }
        if (cyclomaticMetrics.isEmpty() || cognitiveMetrics.isEmpty()) {
            return unavailable(
                    AnalysisScoringResult.Availability.INSUFFICIENT_DATA,
                    "Both cyclomatic and cognitive complexity metrics are required.");
        }

        Map<String, BigDecimal> deductions = new LinkedHashMap<>();
        Set<String> seenMetrics = new HashSet<>();
        BigDecimal cyclomaticDeduction = metricDeductions(
                cyclomaticMetrics,
                policy.getCyclomaticThreshold(),
                policy.getCyclomaticDeductionPerPoint(),
                policy.getCyclomaticMaximumDeduction(),
                seenMetrics);
        BigDecimal cognitiveDeduction = metricDeductions(
                cognitiveMetrics,
                policy.getCognitiveThreshold(),
                policy.getCognitiveDeductionPerPoint(),
                policy.getCognitiveMaximumDeduction(),
                seenMetrics);
        deductions.put(CYCLOMATIC_METRIC, cyclomaticDeduction);
        deductions.put(COGNITIVE_METRIC, cognitiveDeduction);

        Set<String> seenFindings = new HashSet<>();
        BigDecimal severityDeduction = BigDecimal.ZERO;
        for (AnalyzerFinding finding : result.findings()) {
            if (!seenFindings.add(findingIdentity(finding))) {
                continue;
            }
            if (isComplexityFinding(finding)
                    && hasMatchingMetric(finding, result.metrics())) {
                continue;
            }
            if (isComplexityFinding(finding)) {
                BigDecimal findingDeduction = complexityFindingDeduction(finding);
                if (findingDeduction == null) {
                    return unavailable(
                            AnalysisScoringResult.Availability.INSUFFICIENT_DATA,
                            "A complexity finding is missing measurable impact data.");
                }
                String key = finding.ruleId().equals(CYCLOMATIC_RULE)
                        ? CYCLOMATIC_METRIC
                        : COGNITIVE_METRIC;
                deductions.merge(key, findingDeduction, BigDecimal::add);
                continue;
            }
            if (finding.severity() == null) {
                return unavailable(
                        AnalysisScoringResult.Availability.INSUFFICIENT_DATA,
                        "A finding has no severity or supported impact rule.");
            }
            BigDecimal configuredDeduction = policy.getSeverityDeductions()
                    .get(finding.severity().name());
            if (configuredDeduction == null) {
                return unavailable(
                        AnalysisScoringResult.Availability.INSUFFICIENT_DATA,
                        "A finding severity has no configured scoring weight.");
            }
            severityDeduction = severityDeduction.add(configuredDeduction);
        }
        deductions.put("severity", severityDeduction);

        BigDecimal totalDeduction = deductions.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal score = MAXIMUM_SCORE.subtract(totalDeduction)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return new AnalysisScoringResult(
                AnalysisScoringResult.Availability.AVAILABLE,
                policy.getVersion(),
                score,
                gradeFor(score),
                "Score is calculated from available complexity metrics and supported findings.",
                deductions);
    }

    private BigDecimal metricDeductions(
            List<AnalyzerMetric> metrics,
            BigDecimal threshold,
            BigDecimal deductionPerPoint,
            BigDecimal maximumDeduction,
            Set<String> seen) {
        BigDecimal total = BigDecimal.ZERO;
        for (AnalyzerMetric metric : metrics) {
            String identity = metricIdentity(metric);
            if (!seen.add(identity)) {
                continue;
            }
            BigDecimal value = metric.numericValue();
            if (value == null || value.signum() < 0) {
                throw new IllegalArgumentException(
                        "Complexity metric values must be non-negative.");
            }
            BigDecimal deduction = value.subtract(threshold)
                    .max(BigDecimal.ZERO)
                    .multiply(deductionPerPoint)
                    .min(maximumDeduction);
            total = total.add(deduction);
        }
        return total;
    }

    private BigDecimal complexityFindingDeduction(AnalyzerFinding finding) {
        Object measured = finding.metadata().get("measuredValue");
        Object threshold = finding.metadata().get("threshold");
        if (!(measured instanceof Number measuredValue)
                || !(threshold instanceof Number thresholdValue)) {
            return null;
        }
        BigDecimal value = new BigDecimal(measuredValue.toString());
        BigDecimal limit = new BigDecimal(thresholdValue.toString());
        if (value.signum() < 0 || limit.signum() < 0) {
            return null;
        }
        boolean cyclomatic = finding.ruleId().equals(CYCLOMATIC_RULE);
        BigDecimal deductionPerPoint = cyclomatic
                ? policy.getCyclomaticDeductionPerPoint()
                : policy.getCognitiveDeductionPerPoint();
        BigDecimal maximumDeduction = cyclomatic
                ? policy.getCyclomaticMaximumDeduction()
                : policy.getCognitiveMaximumDeduction();
        return value.subtract(limit)
                .max(BigDecimal.ZERO)
                .multiply(deductionPerPoint)
                .min(maximumDeduction);
    }

    private static boolean isComplexityFinding(AnalyzerFinding finding) {
        return CYCLOMATIC_RULE.equals(finding.ruleId())
                || COGNITIVE_RULE.equals(finding.ruleId());
    }

    private static boolean hasMatchingMetric(
            AnalyzerFinding finding,
            List<AnalyzerMetric> metrics) {
        String metricKey = finding.ruleId().equals(CYCLOMATIC_RULE)
                ? CYCLOMATIC_METRIC
                : COGNITIVE_METRIC;
        return metrics.stream().anyMatch(metric ->
                COMPLEXITY_DIMENSION.equals(metric.dimension())
                        && metricKey.equals(metric.key())
                        && equalsNullable(
                                finding.filePath(),
                                metric.metadata().get("sourceName"))
                        && equalsNullable(
                                finding.startLine(),
                                integerMetadata(metric, "startLine"))
                        && equalsNullable(
                                finding.startColumn(),
                                integerMetadata(metric, "startColumn"))
                        && equalsNullable(
                                finding.endLine(),
                                integerMetadata(metric, "endLine"))
                        && equalsNullable(
                                finding.endColumn(),
                                integerMetadata(metric, "endColumn")));
    }

    private static Integer integerMetadata(AnalyzerMetric metric, String key) {
        Object value = metric.metadata().get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private static boolean equalsNullable(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private static String metricIdentity(AnalyzerMetric metric) {
        return metric.key() + "\u0000"
                + metric.metadata().get("sourceName") + "\u0000"
                + metric.metadata().get("startLine") + "\u0000"
                + metric.metadata().get("startColumn") + "\u0000"
                + metric.metadata().get("endLine") + "\u0000"
                + metric.metadata().get("endColumn");
    }

    private static String findingIdentity(AnalyzerFinding finding) {
        if (finding.fingerprint() != null && !finding.fingerprint().isBlank()) {
            return finding.fingerprint();
        }
        return finding.ruleId() + "\u0000"
                + finding.filePath() + "\u0000"
                + finding.startLine() + "\u0000"
                + finding.startColumn() + "\u0000"
                + finding.endLine() + "\u0000"
                + finding.endColumn();
    }

    private String gradeFor(BigDecimal score) {
        if (score.compareTo(policy.getGradeAMinimum()) >= 0) {
            return "A";
        }
        if (score.compareTo(policy.getGradeBMinimum()) >= 0) {
            return "B";
        }
        if (score.compareTo(policy.getGradeCMinimum()) >= 0) {
            return "C";
        }
        if (score.compareTo(policy.getGradeDMinimum()) >= 0) {
            return "D";
        }
        return "F";
    }

    private AnalysisScoringResult unavailable(
            AnalysisScoringResult.Availability availability,
            String explanation) {
        return new AnalysisScoringResult(
                availability,
                policy.getVersion(),
                null,
                null,
                explanation,
                Map.of());
    }
}
