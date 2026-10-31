package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.AnalyzerFinding;
import com.auditor.analysis.contract.AnalyzerMetric;
import com.auditor.analysis.contract.CodeAnalyzer;
import com.auditor.analysis.contract.ParseOutcome;
import com.auditor.analysis.engine.AnalysisInput;
import com.auditor.analysis.engine.AnalysisResult;
import com.auditor.analysis.engine.AnalysisStatus;
import com.auditor.analysis.engine.AnalyzerRegistry;
import com.auditor.analysis.engine.Finding;
import com.auditor.analysis.engine.Metric;
import com.auditor.analysis.engine.SourceLocation;
import com.auditor.analysis.engine.SyntaxDiagnostic;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EngineCodeAnalyzerAdapter implements CodeAnalyzer {

    private final AnalyzerRegistry analyzerRegistry;

    public EngineCodeAnalyzerAdapter(AnalyzerRegistry analyzerRegistry) {
        this.analyzerRegistry = analyzerRegistry;
    }

    @Override
    public AnalysisEngineResult analyze(String normalizedJavaSource) {
        AnalysisResult result =
                analyzerRegistry.analyze(new AnalysisInput(normalizedJavaSource));
        if (result.status() == AnalysisStatus.PARSE_FAILED) {
            return new AnalysisEngineResult(
                    ParseOutcome.FAILED,
                    null,
                    AnalyzerExecutionStatus.PARSE_FAILED,
                    diagnosticSummary(result.diagnostics()),
                    List.of(),
                    List.of());
        }

        return new AnalysisEngineResult(
                ParseOutcome.SUCCEEDED,
                null,
                AnalyzerExecutionStatus.COMPLETED,
                null,
                result.findings().stream()
                        .map(EngineCodeAnalyzerAdapter::toContractFinding)
                        .toList(),
                result.metrics().stream()
                        .map(EngineCodeAnalyzerAdapter::toContractMetric)
                        .toList());
    }

    private static AnalyzerFinding toContractFinding(Finding finding) {
        SourceLocation location = finding.location();
        return new AnalyzerFinding(
                finding.ruleId(),
                fingerprint(finding.ruleId(), location),
                null,
                finding.message(),
                null,
                location.sourceName(),
                positiveOrNull(location.startLine()),
                positiveOrNull(location.startColumn()),
                positiveOrNull(location.endLine()),
                positiveOrNull(location.endColumn()),
                null,
                Map.of(
                        "measuredValue", finding.measuredValue(),
                        "threshold", finding.threshold()));
    }

    private static AnalyzerMetric toContractMetric(Metric metric) {
        Map<String, Object> metadata =
                new LinkedHashMap<>(locationMetadata(metric.location()));
        metadata.put("analyzerId", metric.analyzerId());
        return new AnalyzerMetric(
                metric.analyzerId(),
                metric.name(),
                BigDecimal.valueOf(metric.value()),
                metadata);
    }

    private static Map<String, Object> locationMetadata(SourceLocation location) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceName", location.sourceName());
        metadata.put("startLine", location.startLine());
        metadata.put("startColumn", location.startColumn());
        metadata.put("endLine", location.endLine());
        metadata.put("endColumn", location.endColumn());
        return metadata;
    }

    private static String diagnosticSummary(List<SyntaxDiagnostic> diagnostics) {
        return diagnostics.stream()
                .map(diagnostic -> formatDiagnostic(
                        diagnostic.message(), diagnostic.location()))
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private static String formatDiagnostic(
            String message,
            SourceLocation location) {
        StringBuilder summary = new StringBuilder();
        if (location.sourceName() != null && !location.sourceName().isBlank()) {
            summary.append(location.sourceName());
        }
        if (location.startLine() > 0) {
            if (!summary.isEmpty()) {
                summary.append(':');
            }
            summary.append(location.startLine());
            if (location.startColumn() > 0) {
                summary.append(':').append(location.startColumn());
            }
        }
        if (!summary.isEmpty()) {
            summary.append(": ");
        }
        return summary.append(message).toString();
    }

    private static Integer positiveOrNull(int value) {
        return value > 0 ? value : null;
    }

    private static String fingerprint(String ruleId, SourceLocation location) {
        String identity = String.join(
                "\u0000",
                ruleId,
                String.valueOf(location.sourceName()),
                Integer.toString(location.startLine()),
                Integer.toString(location.startColumn()),
                Integer.toString(location.endLine()),
                Integer.toString(location.endColumn()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    identity.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
