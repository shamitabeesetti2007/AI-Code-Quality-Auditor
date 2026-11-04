package com.auditor.analysis.engine;

public record MaintainabilityThresholds(
        int longMethodLines,
        int maxParameters,
        int maxNestingDepth,
        int maxClassMethods) {

    public MaintainabilityThresholds {
        if (longMethodLines < 1
                || maxParameters < 0
                || maxNestingDepth < 0
                || maxClassMethods < 1) {
            throw new IllegalArgumentException(
                    "maintainability thresholds are out of range");
        }
    }

    public static MaintainabilityThresholds defaults() {
        return new MaintainabilityThresholds(50, 5, 4, 20);
    }
}
