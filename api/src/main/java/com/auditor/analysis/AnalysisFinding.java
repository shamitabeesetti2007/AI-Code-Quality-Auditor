package com.auditor.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

@Entity
@Table(name = "analysis_finding")
public class AnalysisFinding {

    @Id
    @Column(name = "finding_id", nullable = false, updatable = false)
    private UUID findingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    private AnalysisRun run;

    @Column(name = "rule_id", nullable = false, length = 80)
    private String ruleId;

    @Column(
            name = "fingerprint",
            nullable = false,
            length = 64,
            columnDefinition = "CHAR(64)")
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private FindingSeverity severity;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "start_column")
    private Integer startColumn;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "end_column")
    private Integer endColumn;

    @Column(name = "cwe_id", length = 20)
    private String cweId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    protected AnalysisFinding() {
    }

    public AnalysisFinding(
            AnalysisRun run,
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
        this.findingId = UUID.randomUUID();
        this.run = run;
        this.ruleId = ruleId;
        this.fingerprint = fingerprint;
        this.severity = severity;
        this.message = message;
        this.description = description;
        this.filePath = filePath;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.cweId = cweId;
        this.metadata = Collections.unmodifiableMap(
                new LinkedHashMap<>(metadata == null ? Map.of() : metadata));
    }

    public UUID getFindingId() {
        return findingId;
    }

    public UUID getRunId() {
        return run.getRunId();
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public FindingSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    public String getFilePath() {
        return filePath;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public Integer getStartColumn() {
        return startColumn;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public Integer getEndColumn() {
        return endColumn;
    }

    public String getCweId() {
        return cweId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
