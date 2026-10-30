package com.auditor.analysis.engine;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisEngineTest {

    private final JavaSourceParser parser = new JavaSourceParser();
    private final ComplexityAnalyzer complexity = new ComplexityAnalyzer(
            new ComplexityThresholds(10, 15));

    @Test
    void parsesValidJavaSource() {
        JavaParseResult result = parser.parse(new AnalysisInput(
                "Example.java", "class Example { int value() { return 7; } }"));

        assertTrue(result.successful());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void invalidJavaReturnsStructuredLocationAndSuppressesAnalyzers() {
        AtomicInteger calls = new AtomicInteger();
        Analyzer countingAnalyzer = new Analyzer() {
            @Override
            public String analyzerId() {
                return "counting";
            }

            @Override
            public AnalyzerResult analyze(AnalysisContext context) {
                calls.incrementAndGet();
                return AnalyzerResult.empty();
            }
        };
        AnalyzerRegistry registry = new AnalyzerRegistry(parser, List.of(countingAnalyzer));

        AnalysisResult result = registry.analyze(new AnalysisInput(
                "Broken.java", "class Broken {\n  void missing( {\n}"));

        assertEquals(AnalysisStatus.PARSE_FAILED, result.status());
        assertEquals("PARSE_FAILED", result.canonicalRunStatusName());
        assertFalse(result.diagnostics().isEmpty());
        assertEquals("Broken.java", result.diagnostics().getFirst().location().sourceName());
        assertTrue(result.diagnostics().getFirst().location().startLine() > 0);
        assertEquals(0, calls.get());
        assertTrue(result.metrics().isEmpty());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void simpleMethodHasBaselineCyclomaticAndZeroCognitiveComplexity() {
        AnalysisResult result = analyze("class Example { void run() {} }");

        assertEquals(1.0, metric(result, "cyclomatic.complexity"));
        assertEquals(0.0, metric(result, "cognitive.complexity"));
    }

    @Test
    void nestedIfElseCountsBranchesAndNesting() {
        AnalysisResult result = analyze("class Example { void run(boolean a, boolean b) {"
                + " if (a) { if (b) {} } else {} } }");

        assertEquals(3.0, metric(result, "cyclomatic.complexity"));
        assertEquals(4.0, metric(result, "cognitive.complexity"));
    }

    @Test
    void loopsCountCyclomaticAndCognitiveIncrements() {
        AnalysisResult result = analyze("class Example { void run() {"
                + " for (;;) { while (true) { do {} while (false); } } } }");

        assertEquals(4.0, metric(result, "cyclomatic.complexity"));
        assertEquals(6.0, metric(result, "cognitive.complexity"));
    }

    @Test
    void switchCountsNonDefaultLabelsAndOneCognitiveBreak() {
        AnalysisResult result = analyze("class Example { int run(int n) { switch (n) {"
                + " case 1: return 1; case 2: return 2; default: return 0; } } }");

        assertEquals(3.0, metric(result, "cyclomatic.complexity"));
        assertEquals(1.0, metric(result, "cognitive.complexity"));
    }

    @Test
    void ternaryAndLogicalOperatorsAreCounted() {
        AnalysisResult ternary = analyze("class Example { int run(boolean a) {"
                + " return a ? 1 : 0; } }");
        AnalysisResult logical = analyze("class Example { boolean run(boolean a, boolean b, boolean c) {"
                + " return a && b || c; } }");

        assertEquals(2.0, metric(ternary, "cyclomatic.complexity"));
        assertEquals(1.0, metric(ternary, "cognitive.complexity"));
        assertEquals(3.0, metric(logical, "cyclomatic.complexity"));
        assertEquals(2.0, metric(logical, "cognitive.complexity"));
    }

    @Test
    void recursiveCallAddsCognitiveComplexityOnly() {
        AnalysisResult result = analyze("class Example { int run(int n) {"
                + " return n <= 1 ? 1 : n * run(n - 1); } }");

        assertEquals(2.0, metric(result, "cyclomatic.complexity"));
        assertEquals(2.0, metric(result, "cognitive.complexity"));
    }

    @Test
    void classMetricsAggregateMethodsAndConstructors() {
        AnalysisResult result = analyze("class Example { Example() {} void one() {}"
                + " void two(boolean flag) { if (flag) {} } }");

        assertEquals(4.0, metric(result, "cyclomatic.complexity.aggregate"));
        assertEquals(1.0, metric(result, "cognitive.complexity.aggregate"));
        assertEquals(3, result.metrics().stream()
                .filter(metric -> metric.name().equals("cyclomatic.complexity"))
                .count());
    }

    @Test
    void formattingDoesNotChangeMetricsAndFindingsAreStable() {
        ComplexityAnalyzer lowThresholdAnalyzer = new ComplexityAnalyzer(
            new ComplexityThresholds(1, 1));
        AnalyzerRegistry registry = new AnalyzerRegistry(parser, List.of(lowThresholdAnalyzer));
        AnalysisInput compact = new AnalysisInput("class Example{void run(boolean a){if(a){}}}");
        AnalysisInput formatted = new AnalysisInput("class Example {\n  void run(boolean a) {\n"
                + "    if (a) { }\n  }\n}");
        AnalysisResult first = registry.analyze(compact);
        AnalysisResult reformatted = registry.analyze(formatted);
        AnalysisResult repeated = registry.analyze(compact);

        assertEquals(metric(first, "cyclomatic.complexity"), metric(reformatted, "cyclomatic.complexity"));
        assertEquals(metric(first, "cognitive.complexity"), metric(reformatted, "cognitive.complexity"));
        assertEquals(first.findings(), repeated.findings());
        assertEquals(1, first.findings().size());
        assertEquals(ComplexityAnalyzer.CYCLOMATIC_RULE_ID, first.findings().getFirst().ruleId());
        assertEquals(2.0, first.findings().getFirst().measuredValue());
    }

    private AnalysisResult analyze(String source) {
        return new AnalyzerRegistry(parser, List.of(complexity))
                .analyze(new AnalysisInput(source));
    }

    private double metric(AnalysisResult result, String name) {
        return result.metrics().stream()
                .filter(metric -> metric.name().equals(name))
                .mapToDouble(Metric::value)
                .findFirst()
                .orElseThrow();
    }
}