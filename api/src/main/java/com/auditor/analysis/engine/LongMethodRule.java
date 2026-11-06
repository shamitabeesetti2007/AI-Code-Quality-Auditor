package com.auditor.analysis.engine;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

public class LongMethodRule implements SmellRule {

    public static final String RULE_ID = "MAINTAINABILITY_LONG_METHOD";

    private final MaintainabilityProperties properties;

    public LongMethodRule(MaintainabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (MethodDeclaration method :
                context.compilationUnit().findAll(MethodDeclaration.class)) {
            int count = LogicalStatementCounter.count(method);

            if (count > properties.getLongMethodStatements()) {
                findings.add(new Finding(
                        RULE_ID,
                        "Method exceeds the logical statement limit.",
                        count,
                        properties.getLongMethodStatements(),
                        SourceLocation.of(context.input().sourceName(), method)));
            }
        }

        for (ConstructorDeclaration constructor :
                context.compilationUnit().findAll(ConstructorDeclaration.class)) {
            int count = LogicalStatementCounter.count(constructor);

            if (count > properties.getLongMethodStatements()) {
                findings.add(new Finding(
                        RULE_ID,
                        "Constructor exceeds the logical statement limit.",
                        count,
                        properties.getLongMethodStatements(),
                        SourceLocation.of(context.input().sourceName(), constructor)));
            }
        }

        return new AnalyzerResult(List.of(), findings);
    }
}