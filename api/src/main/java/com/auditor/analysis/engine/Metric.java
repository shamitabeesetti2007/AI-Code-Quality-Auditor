package com.auditor.analysis.engine;

import java.util.Objects;

public record Metric(
        String analyzerId,
        String name,
        double value,
        SourceLocation location) {

    public Metric {
        Objects.requireNonNull(analyzerId, "analyzerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(location, "location must not be null");
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("metric value must be finite");
        }
    }
}