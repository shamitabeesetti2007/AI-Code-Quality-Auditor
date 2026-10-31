package com.auditor.analysis.dto;

import com.auditor.analysis.AnalysisFinding;
import com.auditor.analysis.FindingSeverity;

import java.util.Map;
import java.util.UUID;

public record AnalysisFindingResponse(
        UUID findingId,
        String ruleId,
        String fingerprint,
        FindingSeverity severity,
        String message,
        String description,
        String filePath,
        Integer startLine,
        Integer startColumn,
        Integer endLine,
        Integer endColumn,
        String cweId,
        Map<String, Object> metadata) {

    public static AnalysisFindingResponse from(AnalysisFinding finding) {
        return new AnalysisFindingResponse(
                finding.getFindingId(),
                finding.getRuleId(),
                finding.getFingerprint(),
                finding.getSeverity(),
                finding.getMessage(),
                finding.getDescription(),
                finding.getFilePath(),
                finding.getStartLine(),
                finding.getStartColumn(),
                finding.getEndLine(),
                finding.getEndColumn(),
                finding.getCweId(),
                finding.getMetadata());
    }
}
