package com.auditor.analysis.engine;

import java.util.Objects;

public record Finding(
        String ruleId,
        String message,
        double measuredValue,
        double threshold,
        SourceLocation location) {

    public Finding {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(location, "location must not be null");
    }
}