package com.auditor.analysis.engine;

import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.ArrayList;
import java.util.List;

public class PrintStackTraceRule implements SmellRule {

    public static final String RULE_ID = "MAINTAINABILITY_PRINT_STACK_TRACE";

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (MethodCallExpr call :
                context.compilationUnit().findAll(MethodCallExpr.class)) {

            if (!call.getNameAsString().equals("printStackTrace")
                    || !call.getArguments().isEmpty()) {
                continue;
            }

            findings.add(new Finding(
                    RULE_ID,
                    "Avoid printStackTrace(); use a logging framework.",
                    1,
                    0,
                    SourceLocation.of(context.input().sourceName(), call)));
        }

        return new AnalyzerResult(List.of(), findings);
    }
}