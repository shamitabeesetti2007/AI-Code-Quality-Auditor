package com.auditor.analysis.engine;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ComplexityAnalyzer implements Analyzer {

    public static final String ANALYZER_ID = "java-complexity";
    public static final String CYCLOMATIC_RULE_ID = "JAVA-CYCLOMATIC-COMPLEXITY";
    public static final String COGNITIVE_RULE_ID = "JAVA-COGNITIVE-COMPLEXITY";

    private final ComplexityThresholds thresholds;

    public ComplexityAnalyzer() {
        this(ComplexityThresholds.defaults());
    }

    public ComplexityAnalyzer(ComplexityThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds must not be null");
    }

    @Override
    public String analyzerId() {
        return ANALYZER_ID;
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {
        List<Metric> metrics = new ArrayList<>();
        List<Finding> findings = new ArrayList<>();

        for (ClassOrInterfaceDeclaration type : context.compilationUnit()
                .findAll(ClassOrInterfaceDeclaration.class)) {
            int cyclomaticTotal = 0;
            int cognitiveTotal = 0;
            for (MethodDeclaration method : type.getMethods()) {
                ComplexityValue value = measure(method, method.getNameAsString());
                addCallableMetrics(context, method, value, metrics, findings);
                cyclomaticTotal += value.cyclomatic();
                cognitiveTotal += value.cognitive();
            }
            for (ConstructorDeclaration constructor : type.getConstructors()) {
                ComplexityValue value = measure(constructor, "");
                addCallableMetrics(context, constructor, value, metrics, findings);
                cyclomaticTotal += value.cyclomatic();
                cognitiveTotal += value.cognitive();
            }

            SourceLocation typeLocation = SourceLocation.of(context.input().sourceName(), type);
            metrics.add(new Metric(ANALYZER_ID, "cyclomatic.complexity.aggregate",
                    cyclomaticTotal, typeLocation));
            metrics.add(new Metric(ANALYZER_ID, "cognitive.complexity.aggregate",
                    cognitiveTotal, typeLocation));
        }
        return new AnalyzerResult(metrics, findings);
    }

    private void addCallableMetrics(
            AnalysisContext context,
            CallableDeclaration<?> callable,
            ComplexityValue value,
            List<Metric> metrics,
            List<Finding> findings) {
        SourceLocation location = SourceLocation.of(context.input().sourceName(), callable);
        metrics.add(new Metric(ANALYZER_ID, "cyclomatic.complexity", value.cyclomatic(), location));
        metrics.add(new Metric(ANALYZER_ID, "cognitive.complexity", value.cognitive(), location));
        if (value.cyclomatic() > thresholds.cyclomatic()) {
            findings.add(new Finding(
                    CYCLOMATIC_RULE_ID,
                    "Cyclomatic complexity " + value.cyclomatic()
                            + " exceeds threshold " + thresholds.cyclomatic() + ".",
                    value.cyclomatic(), thresholds.cyclomatic(), location));
        }
        if (value.cognitive() > thresholds.cognitive()) {
            findings.add(new Finding(
                    COGNITIVE_RULE_ID,
                    "Cognitive complexity " + value.cognitive()
                            + " exceeds threshold " + thresholds.cognitive() + ".",
                    value.cognitive(), thresholds.cognitive(), location));
        }
    }

    private ComplexityValue measure(CallableDeclaration<?> callable, String callableName) {
        ComplexityVisitor visitor = new ComplexityVisitor(callableName);
        if (callable instanceof MethodDeclaration method) {
            method.getBody().ifPresent(body -> body.accept(visitor, null));
        } else if (callable instanceof ConstructorDeclaration constructor) {
            constructor.getBody().accept(visitor, null);
        }
        return new ComplexityValue(1 + visitor.cyclomaticIncrements, visitor.cognitive);
    }

    private record ComplexityValue(int cyclomatic, int cognitive) {
    }

    private static final class ComplexityVisitor extends VoidVisitorAdapter<Void> {

        private final String callableName;
        private int cyclomaticIncrements;
        private int cognitive;
        private int nesting;

        private ComplexityVisitor(String callableName) {
            this.callableName = callableName;
        }

        @Override
        public void visit(IfStmt node, Void unused) {
            cyclomaticIncrements++;
            cognitive += 1 + nesting;
            node.getCondition().accept(this, unused);
            visitNested(node.getThenStmt(), unused);
            node.getElseStmt().ifPresent(elseStmt -> {
                if (elseStmt instanceof IfStmt) {
                    elseStmt.accept(this, unused);
                } else {
                    cognitive++;
                    visitNested(elseStmt, unused);
                }
            });
        }

        @Override
        public void visit(ForStmt node, Void unused) {
            cyclomaticIncrements++;
            cognitive += 1 + nesting;
            node.getInitialization().forEach(expression -> expression.accept(this, unused));
            node.getCompare().ifPresent(expression -> expression.accept(this, unused));
            node.getUpdate().forEach(expression -> expression.accept(this, unused));
            visitNested(node.getBody(), unused);
        }

        @Override
        public void visit(ForEachStmt node, Void unused) {
            cyclomaticIncrements++;
            cognitive += 1 + nesting;
            node.getIterable().accept(this, unused);
            visitNested(node.getBody(), unused);
        }

        @Override
        public void visit(WhileStmt node, Void unused) {
            cyclomaticIncrements++;
            cognitive += 1 + nesting;
            node.getCondition().accept(this, unused);
            visitNested(node.getBody(), unused);
        }

        @Override
        public void visit(DoStmt node, Void unused) {
            cyclomaticIncrements++;
            cognitive += 1 + nesting;
            visitNested(node.getBody(), unused);
            node.getCondition().accept(this, unused);
        }

        @Override
        public void visit(SwitchStmt node, Void unused) {
            cyclomaticIncrements += countCaseLabels(node.getEntries());
            cognitive += 1 + nesting;
            node.getSelector().accept(this, unused);
            nesting++;
            node.getEntries().forEach(entry -> entry.accept(this, unused));
            nesting--;
        }

        @Override
        public void visit(SwitchExpr node, Void unused) {
            cyclomaticIncrements += countCaseLabels(node.getEntries());
            cognitive += 1 + nesting;
            node.getSelector().accept(this, unused);
            nesting++;
            node.getEntries().forEach(entry -> entry.accept(this, unused));
            nesting--;
        }

        @Override
        public void visit(CatchClause node, Void unused) {
            cyclomaticIncrements++;
            cognitive += 1 + nesting;
            visitNested(node.getBody(), unused);
        }

        @Override
        public void visit(ConditionalExpr node, Void unused) {
            cyclomaticIncrements++;
            cognitive += 1 + nesting;
            node.getCondition().accept(this, unused);
            node.getThenExpr().accept(this, unused);
            node.getElseExpr().accept(this, unused);
        }

        @Override
        public void visit(BinaryExpr node, Void unused) {
            if (isLogical(node)) {
                cyclomaticIncrements++;
                if (!(node.getParentNode().orElse(null) instanceof BinaryExpr parent
                        && isLogical(parent)
                        && parent.getOperator() == node.getOperator())) {
                    cognitive++;
                }
            }
            super.visit(node, unused);
        }

        @Override
        public void visit(MethodCallExpr node, Void unused) {
            if (!callableName.isEmpty() && node.getNameAsString().equals(callableName)) {
                cognitive++;
            }
            super.visit(node, unused);
        }

        @Override
        public void visit(LambdaExpr node, Void unused) {
            // Lambda bodies are analyzed as separate executable scopes by a future analyzer.
        }

        private void visitNested(com.github.javaparser.ast.Node node, Void unused) {
            nesting++;
            node.accept(this, unused);
            nesting--;
        }

        private int countCaseLabels(List<SwitchEntry> entries) {
            return entries.stream()
                .mapToInt(entry -> entry.getLabels().size())
                    .sum();
        }

        private boolean isLogical(BinaryExpr expression) {
            return expression.getOperator() == BinaryExpr.Operator.AND
                    || expression.getOperator() == BinaryExpr.Operator.OR;
        }
    }
}