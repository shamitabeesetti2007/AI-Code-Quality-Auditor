package com.auditor.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "analysis_metric")
public class AnalysisMetric {

    @Id
    @Column(name = "metric_id", nullable = false, updatable = false)
    private UUID metricId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    private AnalysisRun run;

    @Column(name = "dimension", nullable = false, length = 40)
    private String dimension;

    @Column(name = "metric_key", nullable = false, length = 80)
    private String metricKey;

    @Column(name = "numeric_value", nullable = false)
    private BigDecimal numericValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    protected AnalysisMetric() {
    }

    public AnalysisMetric(
            AnalysisRun run,
            String dimension,
            String metricKey,
            BigDecimal numericValue,
            Map<String, Object> metadata) {
        this.metricId = UUID.randomUUID();
        this.run = run;
        this.dimension = dimension;
        this.metricKey = metricKey;
        this.numericValue = numericValue;
        this.metadata = Collections.unmodifiableMap(
                new LinkedHashMap<>(metadata == null ? Map.of() : metadata));
    }

    public UUID getMetricId() {
        return metricId;
    }

    public UUID getRunId() {
        return run.getRunId();
    }

    public String getDimension() {
        return dimension;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
