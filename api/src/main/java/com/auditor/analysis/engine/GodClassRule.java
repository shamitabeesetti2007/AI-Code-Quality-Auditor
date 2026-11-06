package com.auditor.analysis.engine;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

public class GodClassRule implements SmellRule {

    public static final String RULE_ID = "MAINTAINABILITY_GOD_CLASS";

    private final MaintainabilityProperties properties;

    public GodClassRule(MaintainabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (ClassOrInterfaceDeclaration type :
                context.compilationUnit().findAll(ClassOrInterfaceDeclaration.class)) {

            int totalComplexity = type.getMethods().stream()
                    .mapToInt(this::cyclomaticComplexity)
                    .sum();

            if (totalComplexity > properties.getGodClassComplexity()) {
                findings.add(new Finding(
                        RULE_ID,
                        "Class exceeds the configured total method cyclomatic complexity.",
                        totalComplexity,
                        properties.getGodClassComplexity(),
                        SourceLocation.of(context.input().sourceName(), type)));
            }
        }

        return new AnalyzerResult(List.of(), findings);
    }

    private int cyclomaticComplexity(MethodDeclaration method) {
        ComplexityCounter counter = new ComplexityCounter();

        method.getBody().ifPresent(body -> body.accept(counter, null));

        return 1 + counter.increments;
    }

    private static class ComplexityCounter extends VoidVisitorAdapter<Void> {

        private int increments;

        @Override
        public void visit(IfStmt node, Void unused) {
            increments++;
            node.getCondition().accept(this, unused);
            node.getThenStmt().accept(this, unused);
            node.getElseStmt().ifPresent(statement -> statement.accept(this, unused));
        }

        @Override
        public void visit(ForStmt node, Void unused) {
            increments++;
            node.getInitialization().forEach(expression -> expression.accept(this, unused));
            node.getCompare().ifPresent(expression -> expression.accept(this, unused));
            node.getUpdate().forEach(expression -> expression.accept(this, unused));
            node.getBody().accept(this, unused);
        }

        @Override
        public void visit(ForEachStmt node, Void unused) {
            increments++;
            node.getIterable().accept(this, unused);
            node.getBody().accept(this, unused);
        }

        @Override
        public void visit(WhileStmt node, Void unused) {
            increments++;
            node.getCondition().accept(this, unused);
            node.getBody().accept(this, unused);
        }

        @Override
        public void visit(DoStmt node, Void unused) {
            increments++;
            node.getBody().accept(this, unused);
            node.getCondition().accept(this, unused);
        }

        @Override
        public void visit(SwitchStmt node, Void unused) {
            increments += countCaseLabels(node.getEntries());
            node.getSelector().accept(this, unused);
            node.getEntries().forEach(entry -> entry.accept(this, unused));
        }

        @Override
        public void visit(com.github.javaparser.ast.expr.SwitchExpr node, Void unused) {
            increments += countCaseLabels(node.getEntries());
            node.getSelector().accept(this, unused);
            node.getEntries().forEach(entry -> entry.accept(this, unused));
        }

        @Override
        public void visit(CatchClause node, Void unused) {
            increments++;
            node.getBody().accept(this, unused);
        }

        @Override
        public void visit(ConditionalExpr node, Void unused) {
            increments++;
            node.getCondition().accept(this, unused);
            node.getThenExpr().accept(this, unused);
            node.getElseExpr().accept(this, unused);
        }

        @Override
        public void visit(BinaryExpr node, Void unused) {
            if (node.getOperator() == BinaryExpr.Operator.AND
                    || node.getOperator() == BinaryExpr.Operator.OR) {
                increments++;
            }
            super.visit(node, unused);
        }

        @Override
        public void visit(LambdaExpr node, Void unused) {
            // Lambda complexity is excluded.
        }

        private int countCaseLabels(List<SwitchEntry> entries) {
            return entries.stream()
                    .mapToInt(entry -> entry.getLabels().size())
                    .sum();
        }
    }
}