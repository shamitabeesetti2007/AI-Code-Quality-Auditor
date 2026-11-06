package com.auditor.analysis.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PmdAnalyzerAdapterTest {

    private final PmdAnalyzerAdapter analyzer = new PmdAnalyzerAdapter();
    private final JavaSourceParser parser = new JavaSourceParser();

    @Test
    void analyzerHasPmdIdentifier() {
        assertEquals("pmd", analyzer.analyzerId());
    }

    @Test
    void detectsViolationAndTagsFindingAsPmd() {
        AnalysisInput input = new AnalysisInput(
                "Example.java",
                """
                class Example {
                    void run() {
                        System.out.println("debug");
                    }
                }
                """
        );

        JavaParseResult parseResult = parser.parse(input);
        assertTrue(parseResult.successful());

        AnalysisContext context = new AnalysisContext(
                input,
                parseResult.compilationUnit().orElseThrow()
        );

        AnalyzerResult result = analyzer.analyze(context);

        assertFalse(result.findings().isEmpty());
        assertTrue(result.findings().stream()
                .anyMatch(finding -> "PMD".equals(finding.source())));

        assertTrue(result.findings().stream()
                .allMatch(finding -> finding.location().sourceName()
                        .equals("Example.java")));

        assertTrue(result.metrics().isEmpty());
    }
}
