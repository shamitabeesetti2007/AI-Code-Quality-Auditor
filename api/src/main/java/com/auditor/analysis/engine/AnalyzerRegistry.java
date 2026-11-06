package com.auditor.analysis.engine;

import com.github.javaparser.ast.CompilationUnit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class AnalyzerRegistry {

    private final JavaSourceParser parser;
    private final List<Analyzer> analyzers;

    public AnalyzerRegistry(JavaSourceParser parser, List<Analyzer> analyzers) {
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.analyzers = analyzers.stream()
                .sorted(Comparator.comparing(Analyzer::analyzerId))
                .toList();
        long distinctIds = this.analyzers.stream()
                .map(Analyzer::analyzerId)
                .distinct()
                .count();
        if (distinctIds != this.analyzers.size()) {
            throw new IllegalArgumentException("analyzer ids must be unique");
        }
    }

    public AnalysisResult analyze(AnalysisInput input) {
        JavaParseResult parsed = parser.parse(input);
        if (!parsed.successful()) {
            return new AnalysisResult(
                    AnalysisStatus.PARSE_FAILED,
                    parsed.diagnostics(),
                    List.of(),
                    List.of());
        }

        CompilationUnit compilationUnit = parsed.compilationUnit().orElseThrow();
        AnalysisContext context = new AnalysisContext(input, compilationUnit);
        List<Metric> metrics = new ArrayList<>();
        List<Finding> findings = new ArrayList<>();
        for (Analyzer analyzer : analyzers) {
            AnalyzerResult result = Objects.requireNonNull(
                    analyzer.analyze(context),
                    "analyzer result must not be null");
            metrics.addAll(result.metrics());
            findings.addAll(result.findings());
        }
        return new AnalysisResult(AnalysisStatus.ANALYZED, List.of(), metrics, findings);
    }
}