package com.auditor.analysis.engine;

public interface SmellRule {

    String ruleId();

    AnalyzerResult analyze(AnalysisContext context);
}