package com.auditor.analysis.engine;

import java.util.Objects;

public record Finding(
        String ruleId,
        String message,
        double measuredValue,
        double threshold,
        SourceLocation location,
        String source) {

    public Finding {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(location, "location must not be null");

        source = source == null || source.isBlank() ? "ENGINE" : source;
    }

    public Finding(
            String ruleId,
            String message,
            double measuredValue,
            double threshold,
            SourceLocation location) {
        this(ruleId, message, measuredValue, threshold, location, "ENGINE");
    }
}