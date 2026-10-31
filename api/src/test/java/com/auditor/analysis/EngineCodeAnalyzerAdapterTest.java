package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.ParseOutcome;
import com.auditor.analysis.engine.AnalyzerRegistry;
import com.auditor.analysis.engine.ComplexityAnalyzer;
import com.auditor.analysis.engine.ComplexityThresholds;
import com.auditor.analysis.engine.JavaSourceParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineCodeAnalyzerAdapterTest {

    private static final String SOURCE =
            "class Example { void run(boolean condition) { if (condition) {} } }";

    private final EngineCodeAnalyzerAdapter adapter = new EngineCodeAnalyzerAdapter(
            new AnalyzerRegistry(
                    new JavaSourceParser(),
                    List.of(new ComplexityAnalyzer(new ComplexityThresholds(1, 0)))));

    @Test
    void mapsSuccessfulFindingsAndMetricsWithoutInventingSeverityOrCwe() {
        AnalysisEngineResult result = adapter.analyze(SOURCE);

        assertEquals(ParseOutcome.SUCCEEDED, result.parseOutcome());
        assertEquals(AnalyzerExecutionStatus.COMPLETED, result.executionStatus());
        assertNull(result.analyzerVersion());
        assertNull(result.errorSummary());

        var finding = result.findings().stream()
                .filter(item -> item.ruleId().equals(
                        ComplexityAnalyzer.CYCLOMATIC_RULE_ID))
                .findFirst()
                .orElseThrow();
        assertEquals("Source.java", finding.filePath());
        assertEquals("Cyclomatic complexity 2 exceeds threshold 1.", finding.message());
        assertNull(finding.description());
        assertNull(finding.severity());
        assertNull(finding.cweId());
        assertTrue(finding.startLine() > 0);
        assertTrue(finding.startColumn() > 0);
        assertTrue(finding.endLine() > 0);
        assertTrue(finding.endColumn() > 0);
        assertEquals(2.0, finding.metadata().get("measuredValue"));
        assertEquals(1.0, finding.metadata().get("threshold"));
        assertEquals(
                expectedFingerprint(
                        finding.ruleId(),
                        finding.filePath(),
                        finding.startLine(),
                        finding.startColumn(),
                        finding.endLine(),
                        finding.endColumn()),
                finding.fingerprint());

        var metric = result.metrics().stream()
                .filter(item -> item.key().equals("cyclomatic.complexity"))
                .findFirst()
                .orElseThrow();
        assertEquals(ComplexityAnalyzer.ANALYZER_ID, metric.dimension());
        assertEquals(2, metric.numericValue().intValueExact());
        assertEquals(ComplexityAnalyzer.ANALYZER_ID, metric.metadata().get("analyzerId"));
        assertEquals("Source.java", metric.metadata().get("sourceName"));
        assertTrue(result.findings().stream()
                .anyMatch(item -> item.ruleId().equals(
                        ComplexityAnalyzer.COGNITIVE_RULE_ID)));
    }

    @Test
    void identicalEngineFindingsReceiveDeterministicFingerprints() {
        AnalysisEngineResult first = adapter.analyze(SOURCE);
        AnalysisEngineResult repeated = adapter.analyze(SOURCE);

        assertEquals(
                first.findings().stream().map(item -> item.fingerprint()).toList(),
                repeated.findings().stream().map(item -> item.fingerprint()).toList());
        assertFalse(first.findings().getFirst().fingerprint().isBlank());
    }

    @Test
    void mapsParseFailureAndDiagnosticLocationIntoSafeSummary() {
        AnalysisEngineResult result =
                adapter.analyze("class Broken {\n  void missing( {\n}");

        assertEquals(ParseOutcome.FAILED, result.parseOutcome());
        assertEquals(AnalyzerExecutionStatus.PARSE_FAILED, result.executionStatus());
        assertTrue(result.errorSummary().contains("Source.java"));
        assertTrue(result.errorSummary().contains(":2:"));
        assertTrue(result.findings().isEmpty());
        assertTrue(result.metrics().isEmpty());
    }

    private static String expectedFingerprint(
            String ruleId,
            String sourceName,
            int startLine,
            int startColumn,
            int endLine,
            int endColumn) {
        String identity = String.join(
                "\u0000",
                ruleId,
                sourceName,
                Integer.toString(startLine),
                Integer.toString(startColumn),
                Integer.toString(endLine),
                Integer.toString(endColumn));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
