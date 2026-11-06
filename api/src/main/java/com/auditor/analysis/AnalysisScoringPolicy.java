package com.auditor.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "auditor.analysis.scoring")
public class AnalysisScoringPolicy {

    private String version = "complexity-v1";
    private BigDecimal gradeAMinimum = new BigDecimal("90");
    private BigDecimal gradeBMinimum = new BigDecimal("80");
    private BigDecimal gradeCMinimum = new BigDecimal("70");
    private BigDecimal gradeDMinimum = new BigDecimal("60");
    private Map<String, BigDecimal> severityDeductions = defaultSeverityDeductions();
    private BigDecimal cyclomaticThreshold = new BigDecimal("10");
    private BigDecimal cyclomaticDeductionPerPoint = new BigDecimal("2");
    private BigDecimal cyclomaticMaximumDeduction = new BigDecimal("20");
    private BigDecimal cognitiveThreshold = new BigDecimal("15");
    private BigDecimal cognitiveDeductionPerPoint = BigDecimal.ONE;
    private BigDecimal cognitiveMaximumDeduction = new BigDecimal("15");

    public void validate() {
        if (version == null || version.isBlank()) {
            throw new IllegalStateException("Scoring policy version is required.");
        }
        if (!isDescendingGradeScale()) {
            throw new IllegalStateException(
                    "Scoring grade thresholds must descend from 90 to 60.");
        }
        validateMetricPolicy(
                cyclomaticThreshold,
                cyclomaticDeductionPerPoint,
                cyclomaticMaximumDeduction);
        validateMetricPolicy(
                cognitiveThreshold,
                cognitiveDeductionPerPoint,
                cognitiveMaximumDeduction);
        if (severityDeductions == null
                || severityDeductions.entrySet().stream().anyMatch(entry ->
                        entry.getKey() == null
                                || entry.getValue() == null
                                || entry.getValue().signum() < 0)) {
            throw new IllegalStateException(
                    "Severity deductions must be non-negative values.");
        }
    }

    private boolean isDescendingGradeScale() {
        return gradeAMinimum != null
                && gradeBMinimum != null
                && gradeCMinimum != null
                && gradeDMinimum != null
                && gradeAMinimum.compareTo(BigDecimal.valueOf(100)) <= 0
                && gradeAMinimum.compareTo(gradeBMinimum) > 0
                && gradeBMinimum.compareTo(gradeCMinimum) > 0
                && gradeCMinimum.compareTo(gradeDMinimum) > 0
                && gradeDMinimum.signum() >= 0;
    }

    private static void validateMetricPolicy(
            BigDecimal threshold,
            BigDecimal deductionPerPoint,
            BigDecimal maximumDeduction) {
        if (threshold == null
                || threshold.signum() < 0
                || deductionPerPoint == null
                || deductionPerPoint.signum() < 0
                || maximumDeduction == null
                || maximumDeduction.signum() < 0) {
            throw new IllegalStateException(
                    "Complexity scoring thresholds and deductions must be non-negative.");
        }
    }

    private static Map<String, BigDecimal> defaultSeverityDeductions() {
        Map<String, BigDecimal> deductions = new LinkedHashMap<>();
        deductions.put("INFO", BigDecimal.ZERO);
        deductions.put("LOW", new BigDecimal("3"));
        deductions.put("MEDIUM", new BigDecimal("8"));
        deductions.put("HIGH", new BigDecimal("15"));
        deductions.put("CRITICAL", new BigDecimal("25"));
        return deductions;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public BigDecimal getGradeAMinimum() {
        return gradeAMinimum;
    }

    public void setGradeAMinimum(BigDecimal gradeAMinimum) {
        this.gradeAMinimum = gradeAMinimum;
    }

    public BigDecimal getGradeBMinimum() {
        return gradeBMinimum;
    }

    public void setGradeBMinimum(BigDecimal gradeBMinimum) {
        this.gradeBMinimum = gradeBMinimum;
    }

    public BigDecimal getGradeCMinimum() {
        return gradeCMinimum;
    }

    public void setGradeCMinimum(BigDecimal gradeCMinimum) {
        this.gradeCMinimum = gradeCMinimum;
    }

    public BigDecimal getGradeDMinimum() {
        return gradeDMinimum;
    }

    public void setGradeDMinimum(BigDecimal gradeDMinimum) {
        this.gradeDMinimum = gradeDMinimum;
    }

    public Map<String, BigDecimal> getSeverityDeductions() {
        return severityDeductions;
    }

    public void setSeverityDeductions(Map<String, BigDecimal> severityDeductions) {
        this.severityDeductions = severityDeductions;
    }

    public BigDecimal getCyclomaticThreshold() {
        return cyclomaticThreshold;
    }

    public void setCyclomaticThreshold(BigDecimal cyclomaticThreshold) {
        this.cyclomaticThreshold = cyclomaticThreshold;
    }

    public BigDecimal getCyclomaticDeductionPerPoint() {
        return cyclomaticDeductionPerPoint;
    }

    public void setCyclomaticDeductionPerPoint(
            BigDecimal cyclomaticDeductionPerPoint) {
        this.cyclomaticDeductionPerPoint = cyclomaticDeductionPerPoint;
    }

    public BigDecimal getCyclomaticMaximumDeduction() {
        return cyclomaticMaximumDeduction;
    }

    public void setCyclomaticMaximumDeduction(
            BigDecimal cyclomaticMaximumDeduction) {
        this.cyclomaticMaximumDeduction = cyclomaticMaximumDeduction;
    }

    public BigDecimal getCognitiveThreshold() {
        return cognitiveThreshold;
    }

    public void setCognitiveThreshold(BigDecimal cognitiveThreshold) {
        this.cognitiveThreshold = cognitiveThreshold;
    }

    public BigDecimal getCognitiveDeductionPerPoint() {
        return cognitiveDeductionPerPoint;
    }

    public void setCognitiveDeductionPerPoint(
            BigDecimal cognitiveDeductionPerPoint) {
        this.cognitiveDeductionPerPoint = cognitiveDeductionPerPoint;
    }

    public BigDecimal getCognitiveMaximumDeduction() {
        return cognitiveMaximumDeduction;
    }

    public void setCognitiveMaximumDeduction(
            BigDecimal cognitiveMaximumDeduction) {
        this.cognitiveMaximumDeduction = cognitiveMaximumDeduction;
    }
}
