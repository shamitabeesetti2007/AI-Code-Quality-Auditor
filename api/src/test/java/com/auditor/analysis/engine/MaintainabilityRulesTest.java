package com.auditor.analysis.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaintainabilityRulesTest {

    private final JavaSourceParser parser = new JavaSourceParser();

    private MaintainabilityProperties properties() {
        return new MaintainabilityProperties();
    }

    private int findings(SmellRule rule, String source) {
        JavaParseResult parsed = parser.parse(
                new AnalysisInput("Example.java", source));

        if (!parsed.successful()) {
            throw new AssertionError("Invalid test source: " + parsed.diagnostics());
        }

        AnalysisContext context = new AnalysisContext(
                new AnalysisInput("Example.java", source),
                parsed.compilationUnit().orElseThrow());

        return rule.analyze(context).findings().size();
    }

    // LONG METHOD

    @Test
    void longMethodDoesNotFlagExactlyFortyStatements() {
        assertEquals(0, findings(
                new LongMethodRule(properties()),
                methodWithStatements(40)));
    }

    @Test
    void longMethodFlagsFortyOneStatements() {
        assertEquals(1, findings(
                new LongMethodRule(properties()),
                methodWithStatements(41)));
    }

    @Test
    void longMethodDoesNotCountStatementsInsideLambda() {
        String source = """
                class Example {
                    void run() {
                        Runnable task = () -> {
                            %s
                        };
                    }
                }
                """.formatted("value++;".repeat(45));

        assertEquals(0, findings(
                new LongMethodRule(properties()), source));
    }

    // LONG PARAMETER LIST

    @Test
    void parameterListAllowsExactlyFiveParameters() {
        assertEquals(0, findings(
                new LongParameterListRule(properties()),
                "class Example { void run(int a,int b,int c,int d,int e) {} }"));
    }

    @Test
    void parameterListFlagsSixParameters() {
        assertEquals(1, findings(
                new LongParameterListRule(properties()),
                "class Example { void run(int a,int b,int c,int d,int e,int f) {} }"));
    }

    @Test
    void parameterListAlsoChecksConstructors() {
        assertEquals(1, findings(
                new LongParameterListRule(properties()),
                "class Example { Example(int a,int b,int c,int d,int e,int f) {} }"));
    }

    // DEEP NESTING

    @Test
    void nestingAllowsExactlyFourLevels() {
        assertEquals(0, findings(
                new DeepNestingRule(properties()),
                nestedIfSource(4)));
    }

    @Test
    void nestingFlagsFiveLevels() {
        assertEquals(1, findings(
                new DeepNestingRule(properties()),
                nestedIfSource(5)));
    }

    @Test
    void nestingIgnoresNestedLambdaBody() {
        String source = """
                class Example {
                    void run() {
                        Runnable task = () -> {
                            if (a) {
                                if (b) {
                                    if (c) {
                                        if (d) {
                                            if (e) {}
                                        }
                                    }
                                }
                            }
                        };
                    }
                }
                """;

        assertEquals(0, findings(
                new DeepNestingRule(properties()), source));
    }

    // MAGIC NUMBER

    @Test
    void magicNumberFlagsOrdinaryNumericLiteral() {
        assertEquals(1, findings(
                new MagicNumberRule(),
                "class Example { int run() { return 42; } }"));
    }

    @Test
    void magicNumberAllowsZeroOneMinusOneAndTwo() {
        assertEquals(0, findings(
                new MagicNumberRule(),
                "class Example { void run() { int a=0,b=1,c=-1,d=2; } }"));
    }

    @Test
    void magicNumberAllowsDeclaredFinalConstants() {
        assertEquals(0, findings(
                new MagicNumberRule(),
                """
                class Example {
                    static final int LIMIT = 42;
                    void run() {
                        final int localLimit = 99;
                    }
                }
                """));
    }

    // EMPTY CATCH

    @Test
    void emptyCatchFlagsEmptyCatchBlock() {
        assertEquals(1, findings(
                new EmptyCatchRule(),
                "class Example { void run() { try {} catch(Exception e) {} } }"));
    }

    @Test
    void emptyCatchDoesNotFlagCatchWithHandlingCode() {
        assertEquals(0, findings(
                new EmptyCatchRule(),
                """
                class Example {
                    void run() {
                        try {} catch(Exception e) {
                            System.out.println("Handled");
                        }
                    }
                }
                """));
    }

    @Test
    void emptyCatchDoesNotFlagNonEmptyCatchBlock() {
        assertEquals(0, findings(
                new EmptyCatchRule(),
                "class Example { void run() { try {} catch(Exception e) { int x=1; } } }"));
    }

    // IGNORED EXCEPTION

    @Test
    void ignoredExceptionFlagsUnusedBroadException() {
        assertEquals(1, findings(
                new IgnoredExceptionRule(),
                """
                class Example {
                    void run() {
                        try {} catch(Exception e) {
                            int ignored = 1;
                        }
                    }
                }
                """));
    }

    @Test
    void ignoredExceptionDoesNotFlagUsedException() {
        assertEquals(0, findings(
                new IgnoredExceptionRule(),
                """
                class Example {
                    void run() {
                        try {} catch(Exception e) {
                            System.out.println(e);
                        }
                    }
                }
                """));
    }

    @Test
    void ignoredExceptionDoesNotFlagNarrowExceptionType() {
        assertEquals(0, findings(
                new IgnoredExceptionRule(),
                """
                class Example {
                    void run() {
                        try {} catch(java.io.IOException e) {
                            int ignored = 1;
                        }
                    }
                }
                """));
    }

    // PRINT STACK TRACE

    @Test
    void printStackTraceFlagsNoArgumentCall() {
        assertEquals(1, findings(
                new PrintStackTraceRule(),
                """
                class Example {
                    void run(Exception e) {
                        e.printStackTrace();
                    }
                }
                """));
    }

    @Test
    void printStackTraceDoesNotFlagCallWithArguments() {
        assertEquals(0, findings(
                new PrintStackTraceRule(),
                """
                class Example {
                    void run(Exception e) {
                        e.printStackTrace(System.out);
                    }
                }
                """));
    }

    @Test
    void printStackTraceDoesNotFlagUnrelatedMethod() {
        assertEquals(0, findings(
                new PrintStackTraceRule(),
                "class Example { void run() { logError(); } void logError() {} }"));
    }

    // SYSTEM.OUT

    @Test
    void systemOutFlagsUsageOutsideMain() {
        assertEquals(1, findings(
                new SystemOutRule(),
                """
                class Example {
                    void run() {
                        System.out.println("Hello");
                    }
                }
                """));
    }

    @Test
    void systemOutAllowsUsageInsideMain() {
        assertEquals(0, findings(
                new SystemOutRule(),
                """
                class Example {
                    public static void main(String[] args) {
                        System.out.println("Hello");
                    }
                }
                """));
    }

    @Test
    void systemOutDoesNotFlagSystemErr() {
        assertEquals(0, findings(
                new SystemOutRule(),
                "class Example { void run() { System.err.println(\"Error\"); } }"));
    }

    // GOD CLASS

    @Test
    void godClassDoesNotFlagTotalComplexityOfExactlyFifty() {
        assertEquals(0, findings(
                new GodClassRule(properties()),
                methodWithIfCount(49)));
    }

    @Test
    void godClassFlagsTotalComplexityOfFiftyOne() {
        assertEquals(1, findings(
                new GodClassRule(properties()),
                methodWithIfCount(50)));
    }

    @Test
    void godClassExcludesConstructorComplexity() {
        String source = """
                class Example {
                    Example(boolean flag) {
                        %s
                    }
                    void run() {}
                }
                """.formatted("if (flag) {} ".repeat(60));

        assertEquals(0, findings(
                new GodClassRule(properties()), source));
    }

    // TEST SOURCE HELPERS

    private String methodWithStatements(int count) {
        return """
                class Example {
                    void run(int value) {
                        %s
                    }
                }
                """.formatted("value++;".repeat(count));
    }

    private String nestedIfSource(int count) {
        String body = "{}";

        for (int i = 0; i < count; i++) {
            body = "if (flag) { " + body + " }";
        }

        return "class Example { void run(boolean flag) { "
                + body + " } }";
    }

    private String methodWithIfCount(int count) {
        return """
                class Example {
                    void run(boolean flag) {
                        %s
                    }
                }
                """.formatted("if (flag) {} ".repeat(count));
    }
}
