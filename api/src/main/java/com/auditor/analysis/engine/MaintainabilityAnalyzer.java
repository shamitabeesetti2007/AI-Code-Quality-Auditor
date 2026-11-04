package com.auditor.analysis.engine;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MaintainabilityAnalyzer implements Analyzer {

    public static final String ANALYZER_ID = "java-maintainability";

    private final MaintainabilityThresholds thresholds;

    public MaintainabilityAnalyzer() {
        this(MaintainabilityThresholds.defaults());
    }

    public MaintainabilityAnalyzer(MaintainabilityThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(
                thresholds, "thresholds must not be null");
    }

    @Override
    public String analyzerId() {
        return ANALYZER_ID;
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {
        List<Metric> metrics = new ArrayList<>();

        for (ClassOrInterfaceDeclaration type :
                context.compilationUnit().findAll(ClassOrInterfaceDeclaration.class)) {

            int methodCount = type.getMethods().size();

            SourceLocation typeLocation =
                    SourceLocation.of(context.input().sourceName(), type);

            addMetric(metrics, "class.method.count", methodCount, typeLocation);
            addMetric(metrics, "class.method-count.violation",
                    methodCount > thresholds.maxClassMethods() ? 1 : 0,
                    typeLocation);

            for (MethodDeclaration method : type.getMethods()) {
                analyzeCallable(context, method, metrics);
            }

            for (ConstructorDeclaration constructor : type.getConstructors()) {
                analyzeCallable(context, constructor, metrics);
            }
        }

        return new AnalyzerResult(metrics, List.of());
    }

    private void analyzeCallable(
            AnalysisContext context,
            CallableDeclaration<?> callable,
            List<Metric> metrics) {

        SourceLocation location =
                SourceLocation.of(context.input().sourceName(), callable);

        int lineCount = callable.getRange()
                .map(range -> range.end.line - range.begin.line + 1)
                .orElse(0);

        int parameterCount = callable.getParameters().size();

        int nestingDepth = 0;

if (callable instanceof MethodDeclaration method) {
    nestingDepth = method.getBody()
            .map(this::maximumNestingDepth)
            .orElse(0);
} else if (callable instanceof ConstructorDeclaration constructor) {
    nestingDepth = maximumNestingDepth(constructor.getBody());
}

        addMetric(metrics, "callable.lines", lineCount, location);
        addMetric(metrics, "callable.parameters", parameterCount, location);
        addMetric(metrics, "callable.nesting.depth", nestingDepth, location);

        addMetric(metrics, "callable.long-method.violation",
                lineCount > thresholds.longMethodLines() ? 1 : 0, location);

        addMetric(metrics, "callable.parameter-count.violation",
                parameterCount > thresholds.maxParameters() ? 1 : 0, location);

        addMetric(metrics, "callable.nesting-depth.violation",
                nestingDepth > thresholds.maxNestingDepth() ? 1 : 0, location);
    }

    private int maximumNestingDepth(Node node) {
        if (node instanceof LambdaExpr) {
            return 0;
        }

        int maximum = 0;

        for (Node child : node.getChildNodes()) {
            if (child instanceof LambdaExpr) {
                continue;
            }

            int childDepth = maximumNestingDepth(child);
            if (isNestingNode(child)) {
                childDepth++;
            }

            maximum = Math.max(maximum, childDepth);
        }

        return maximum;
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

    private void addMetric(
            List<Metric> metrics,
            String name,
            double value,
            SourceLocation location) {

        metrics.add(new Metric(ANALYZER_ID, name, value, location));
    }
}
