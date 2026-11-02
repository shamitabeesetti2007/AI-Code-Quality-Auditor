package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.AnalyzerFinding;
import com.auditor.analysis.contract.AnalyzerMetric;
import com.auditor.analysis.contract.AnalyzerSeverity;
import com.auditor.analysis.contract.ParseOutcome;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnalysisScoringServiceTest {

    @Test
    void assignsConfiguredGradeBoundaries() {
        AnalysisScoringPolicy policy = new AnalysisScoringPolicy();
        AnalysisScoringService service = new AnalysisScoringService(policy);
        for (GradeCase testCase : List.of(
                new GradeCase("10", "A"),
                new GradeCase("20", "B"),
                new GradeCase("30", "C"),
                new GradeCase("40", "D"),
                new GradeCase("41", "F"))) {
            policy.setSeverityDeductions(Map.of(
                    "LOW", new BigDecimal(testCase.deduction())));

            var result = service.score(result(
                    AnalyzerExecutionStatus.COMPLETED,
                    List.of(finding("same", AnalyzerSeverity.LOW, "RULE")),
                    metrics("10", "15")));

            BigDecimal expectedScore = new BigDecimal("100")
                    .subtract(new BigDecimal(testCase.deduction()))
                    .setScale(2);
            assertEquals(expectedScore, result.score());
            assertEquals(testCase.grade(), result.grade());
        }
    }

    @Test
    void combinesComplexityAndSeverityDeductions() {
        var result = scorer().score(result(
                AnalyzerExecutionStatus.COMPLETED,
                List.of(finding("critical-1", AnalyzerSeverity.HIGH, "SECURITY-RULE")),
                metrics("12", "16")));

        assertEquals(new BigDecimal("80.00"), result.score());
        assertEquals("B", result.grade());
        assertEquals(new BigDecimal("4"), result.deductions().get(
                "cyclomatic.complexity"));
        assertEquals(new BigDecimal("1"), result.deductions().get(
                "cognitive.complexity"));
        assertEquals(new BigDecimal("15"), result.deductions().get("severity"));
    }

    @Test
    void duplicateFindingsAreDeductedOnlyOnce() {
        var duplicate = finding("same-fingerprint", AnalyzerSeverity.HIGH, "RULE");
        var result = scorer().score(result(
                AnalyzerExecutionStatus.COMPLETED,
                List.of(
                        duplicate,
                        finding("same-fingerprint", AnalyzerSeverity.HIGH, "RULE")),
                metrics("10", "15")));

        assertEquals(new BigDecimal("85.00"), result.score());
    }

    @Test
    void complexityThresholdsAndMetricBasedFindingsAreNotDoubleCounted() {
        var result = scorer().score(result(
                AnalyzerExecutionStatus.COMPLETED,
                List.of(complexityFinding(
                        "complexity", "JAVA-CYCLOMATIC-COMPLEXITY",
                        12, 10, 1, 1, 1, 5)),
                metrics("12", "15")));

        assertEquals(new BigDecimal("96.00"), result.score());
        assertEquals(new BigDecimal("4"), result.deductions().get(
                "cyclomatic.complexity"));

        var atThreshold = scorer().score(result(
                AnalyzerExecutionStatus.COMPLETED,
                List.of(),
                metrics("10", "15")));
        assertEquals(new BigDecimal("100.00"), atThreshold.score());
    }

    @Test
    void scoresKnownComplexityFindingFromItsMeasuredImpactWhenMetricIsAbsent() {
        var result = scorer().score(result(
                AnalyzerExecutionStatus.COMPLETED,
                List.of(complexityFinding(
                        "complexity", "JAVA-CYCLOMATIC-COMPLEXITY",
                        12, 10, 2, 1, 2, 5)),
                metrics("10", "15")));

        assertEquals(new BigDecimal("96.00"), result.score());
        assertEquals(
                0,
                new BigDecimal("4")
                        .compareTo(result.deductions().get("cyclomatic.complexity")));
    }

    @Test
    void unknownSeverityAndMissingMetricsMakeScoreUnavailable() {
        var unknownSeverity = scorer().score(result(
                AnalyzerExecutionStatus.COMPLETED,
                List.of(finding("unknown-severity", null, "UNCLASSIFIED-RULE")),
                metrics("10", "15")));
        assertEquals(
                AnalysisScoringResult.Availability.INSUFFICIENT_DATA,
                unknownSeverity.availability());
        assertNull(unknownSeverity.score());
        assertNull(unknownSeverity.grade());

        var missingMetrics = scorer().score(result(
                AnalyzerExecutionStatus.COMPLETED,
                List.of(),
                List.of()));
        assertEquals(
                AnalysisScoringResult.Availability.INSUFFICIENT_DATA,
                missingMetrics.availability());
        assertNull(missingMetrics.score());
        assertNull(missingMetrics.grade());

        var missingMetricFamily = scorer().score(result(
                AnalyzerExecutionStatus.COMPLETED,
                List.of(),
                List.of(metric("cyclomatic.complexity", "10"))));
        assertEquals(
                AnalysisScoringResult.Availability.INSUFFICIENT_DATA,
                missingMetricFamily.availability());
    }

    @Test
    void noFindingsDoesNotSubstituteForRequiredMetricEvidence() {
        var result = scorer().score(result(
                AnalyzerExecutionStatus.COMPLETED, List.of(), List.of()));

        assertEquals(
                AnalysisScoringResult.Availability.INSUFFICIENT_DATA,
                result.availability());
        assertNull(result.score());
    }

    @Test
    void partialParseFailedAndFailedAnalysesAreNotScored() {
        for (AnalyzerExecutionStatus status : List.of(
                AnalyzerExecutionStatus.PARTIAL,
                AnalyzerExecutionStatus.FAILED,
                AnalyzerExecutionStatus.PARSE_FAILED)) {
            ParseOutcome parseOutcome = status == AnalyzerExecutionStatus.PARSE_FAILED
                    ? ParseOutcome.FAILED
                    : ParseOutcome.SUCCEEDED;
            var result = scorer().score(new AnalysisEngineResult(
                    parseOutcome, "engine", status, null,
                    List.of(), metrics("10", "15")));

            assertEquals(
                    AnalysisScoringResult.Availability.INCOMPLETE_ANALYSIS,
                    result.availability());
            assertNull(result.score());
            assertNull(result.grade());
        }
    }

    @Test
    void scoringResultsAreDeterministic() {
        AnalysisEngineResult input = result(
                AnalyzerExecutionStatus.COMPLETED,
                List.of(finding("one", AnalyzerSeverity.MEDIUM, "RULE")),
                metrics("12", "17"));

        assertEquals(scorer().score(input), scorer().score(input));
    }

    private static AnalysisScoringService scorer() {
        return new AnalysisScoringService(new AnalysisScoringPolicy());
    }

    private static AnalysisEngineResult result(
            AnalyzerExecutionStatus executionStatus,
            List<AnalyzerFinding> findings,
            List<AnalyzerMetric> metrics) {
        return new AnalysisEngineResult(
                ParseOutcome.SUCCEEDED,
                "engine",
                executionStatus,
                null,
                findings,
                metrics);
    }

    private static List<AnalyzerMetric> metrics(
            String cyclomatic,
            String cognitive) {
        return List.of(
                metric("cyclomatic.complexity", cyclomatic),
                metric("cognitive.complexity", cognitive));
    }

    private static AnalyzerMetric metric(String key, String value) {
        return new AnalyzerMetric(
                "java-complexity",
                key,
                new BigDecimal(value),
                Map.of(
                        "sourceName", "Source.java",
                        "startLine", 1,
                        "startColumn", 1,
                        "endLine", 1,
                        "endColumn", 5));
    }

    private static AnalyzerFinding finding(
            String fingerprint,
            AnalyzerSeverity severity,
            String ruleId) {
        return new AnalyzerFinding(
                ruleId,
                fingerprint,
                severity,
                "Finding",
                null,
                "Source.java",
                1,
                1,
                1,
                5,
                null,
                Map.of());
    }

    private static AnalyzerFinding complexityFinding(
            String fingerprint,
            String ruleId,
            double measuredValue,
            double threshold,
            int startLine,
            int startColumn,
            int endLine,
            int endColumn) {
        return new AnalyzerFinding(
                ruleId,
                fingerprint,
                null,
                "Complexity finding",
                null,
                "Source.java",
                startLine,
                startColumn,
                endLine,
                endColumn,
                null,
                Map.of(
                        "measuredValue", measuredValue,
                        "threshold", threshold));
    }

    private record GradeCase(String deduction, String grade) {
    }
}
