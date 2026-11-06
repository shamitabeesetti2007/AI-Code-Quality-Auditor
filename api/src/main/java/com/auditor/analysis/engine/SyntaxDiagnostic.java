package com.auditor.analysis.engine;

import java.util.Objects;

public record SyntaxDiagnostic(String message, SourceLocation location) {

    public SyntaxDiagnostic {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(location, "location must not be null");
    }
}