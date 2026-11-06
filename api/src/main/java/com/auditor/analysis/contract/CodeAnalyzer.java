package com.auditor.analysis.contract;

public interface CodeAnalyzer {

    AnalysisEngineResult analyze(String normalizedJavaSource);
}
