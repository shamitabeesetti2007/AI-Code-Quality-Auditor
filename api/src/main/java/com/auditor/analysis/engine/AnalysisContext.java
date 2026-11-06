package com.auditor.analysis.engine;

import com.github.javaparser.ast.CompilationUnit;

import java.util.Objects;

public record AnalysisContext(AnalysisInput input, CompilationUnit compilationUnit) {

    public AnalysisContext {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(compilationUnit, "compilationUnit must not be null");
    }
}