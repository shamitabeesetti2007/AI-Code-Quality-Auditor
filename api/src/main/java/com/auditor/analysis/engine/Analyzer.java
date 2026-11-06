package com.auditor.analysis.engine;

public interface Analyzer {

    String analyzerId();

    AnalyzerResult analyze(AnalysisContext context);
}