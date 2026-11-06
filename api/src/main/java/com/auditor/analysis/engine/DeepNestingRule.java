package com.auditor.analysis.engine;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.*;

import java.util.ArrayList;
import java.util.List;

public class DeepNestingRule implements SmellRule {

    public static final String RULE_ID = "MAINTAINABILITY_DEEP_NESTING";

    private final MaintainabilityProperties properties;

    public DeepNestingRule(MaintainabilityProperties properties) {
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
            method.getBody().ifPresent(body ->
                    check(context, method, body, findings));
        }

        for (ConstructorDeclaration constructor :
                context.compilationUnit().findAll(ConstructorDeclaration.class)) {
            check(context, constructor, constructor.getBody(), findings);
        }

        return new AnalyzerResult(List.of(), findings);
    }

    private void check(
            AnalysisContext context,
            Node callable,
            Node body,
            List<Finding> findings) {

        int depth = maximumDepth(body);

        if (depth > properties.getMaxNestingDepth()) {
            findings.add(new Finding(
                    RULE_ID,
                    "Callable exceeds the maximum nesting depth.",
                    depth,
                    properties.getMaxNestingDepth(),
                    SourceLocation.of(context.input().sourceName(), callable)));
        }
    }

    private int maximumDepth(Node node) {
        if (isNestedCallableBoundary(node)) {
            return 0;
        }

        int maximum = 0;

        for (Node child : node.getChildNodes()) {
            if (isNestedCallableBoundary(child)) {
                continue;
            }

            int childDepth = maximumDepth(child);
            if (isNestingNode(child)) {
                childDepth++;
            }

            maximum = Math.max(maximum, childDepth);
        }

        return maximum;
    }

    private boolean isNestedCallableBoundary(Node node) {
        return node instanceof LambdaExpr
                || node instanceof TypeDeclaration<?>
                || node instanceof ObjectCreationExpr creation
                        && creation.getAnonymousClassBody().isPresent();
    }

    private boolean isNestingNode(Node node) {
        return node instanceof IfStmt
                || node instanceof ForStmt
                || node instanceof ForEachStmt
                || node instanceof WhileStmt
                || node instanceof DoStmt
                || node instanceof SwitchStmt
                || node instanceof SwitchExpr
                || node instanceof CatchClause
                || node instanceof ConditionalExpr;
    }
}