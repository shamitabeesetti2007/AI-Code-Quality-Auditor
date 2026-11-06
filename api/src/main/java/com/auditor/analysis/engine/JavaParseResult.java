package com.auditor.analysis.engine;

import com.github.javaparser.ast.CompilationUnit;

import java.util.List;
import java.util.Optional;

public record JavaParseResult(
        Optional<CompilationUnit> compilationUnit,
        List<SyntaxDiagnostic> diagnostics) {

    public JavaParseResult {
        compilationUnit = compilationUnit == null ? Optional.empty() : compilationUnit;
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean successful() {
        return compilationUnit.isPresent() && diagnostics.isEmpty();
    }
}