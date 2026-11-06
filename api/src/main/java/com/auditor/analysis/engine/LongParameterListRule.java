package com.auditor.analysis.engine;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

public class LongParameterListRule implements SmellRule {

    public static final String RULE_ID = "MAINTAINABILITY_LONG_PARAMETER_LIST";

    private final MaintainabilityProperties properties;

    public LongParameterListRule(MaintainabilityProperties properties) {
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
            int count = method.getParameters().size();

            if (count > properties.getMaxParameters()) {
                findings.add(new Finding(
                        RULE_ID,
                        "Method has too many parameters.",
                        count,
                        properties.getMaxParameters(),
                        SourceLocation.of(context.input().sourceName(), method)));
            }
        }

        for (ConstructorDeclaration constructor :
                context.compilationUnit().findAll(ConstructorDeclaration.class)) {
            int count = constructor.getParameters().size();

            if (count > properties.getMaxParameters()) {
                findings.add(new Finding(
                        RULE_ID,
                        "Constructor has too many parameters.",
                        count,
                        properties.getMaxParameters(),
                        SourceLocation.of(context.input().sourceName(), constructor)));
            }
        }

        return new AnalyzerResult(List.of(), findings);
    }
}