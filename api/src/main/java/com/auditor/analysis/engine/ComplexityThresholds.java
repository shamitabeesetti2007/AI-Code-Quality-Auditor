package com.auditor.analysis.engine;

public record ComplexityThresholds(int cyclomatic, int cognitive) {

    public ComplexityThresholds {
        if (cyclomatic < 1 || cognitive < 0) {
            throw new IllegalArgumentException("complexity thresholds are out of range");
        }
    }

    public static ComplexityThresholds defaults() {
        return new ComplexityThresholds(10, 15);
    }
}