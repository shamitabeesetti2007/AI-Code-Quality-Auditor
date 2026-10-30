package com.auditor.analysis.engine;

import java.util.Objects;

public record AnalysisInput(String sourceName, String source) {

    public AnalysisInput {
        sourceName = sourceName == null || sourceName.isBlank()
                ? "Source.java"
                : sourceName;
        Objects.requireNonNull(source, "source must not be null");
    }

    public AnalysisInput(String source) {
        this("Source.java", source);
    }
}