package com.auditor.analysis.engine;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.util.ArrayList;
import java.util.List;

public class SystemOutRule implements SmellRule {

    public static final String RULE_ID = "MAINTAINABILITY_SYSTEM_OUT";

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (FieldAccessExpr access :
                context.compilationUnit().findAll(FieldAccessExpr.class)) {

            if (!access.getNameAsString().equals("out")
                    || !(access.getScope() instanceof NameExpr scope)
                    || !scope.getNameAsString().equals("System")
                    || isInsideMainMethod(access)) {
                continue;
            }

            findings.add(new Finding(
                    RULE_ID,
                    "Avoid System.out in non-main code; use a logging framework.",
                    1,
                    0,
                    SourceLocation.of(context.input().sourceName(), access)));
        }

        return new AnalyzerResult(List.of(), findings);
    }

    private boolean isInsideMainMethod(Node node) {
        Node current = node;

        while (current != null) {
            if (current instanceof MethodDeclaration method) {
                return method.getNameAsString().equals("main")
                        && method.isStatic()
                        && method.getParameters().size() == 1
                        && method.getParameter(0).getType().asString()
                                .equals("String[]");
            }

            current = current.getParentNode().orElse(null);
        }

        return false;
    }
}