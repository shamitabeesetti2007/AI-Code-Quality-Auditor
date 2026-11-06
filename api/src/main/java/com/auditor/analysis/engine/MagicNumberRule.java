package com.auditor.analysis.engine;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.Modifier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MagicNumberRule implements SmellRule {

    public static final String RULE_ID = "MAINTAINABILITY_MAGIC_NUMBER";

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {
        List<Finding> findings = new ArrayList<>();

        context.compilationUnit().findAll(IntegerLiteralExpr.class)
                .forEach(literal -> inspect(context, literal, findings));

        context.compilationUnit().findAll(LongLiteralExpr.class)
                .forEach(literal -> inspect(context, literal, findings));

        context.compilationUnit().findAll(DoubleLiteralExpr.class)
                .forEach(literal -> inspect(context, literal, findings));

        return new AnalyzerResult(List.of(), findings);
    }

    private void inspect(
            AnalysisContext context,
            Node literal,
            List<Finding> findings) {

        if (isDeclaredConstantInitializer(literal)
                || isAllowedNumber(literal)) {
            return;
        }

        findings.add(new Finding(
                RULE_ID,
                "Numeric literal should be replaced with a named constant.",
                numericValue(literal),
                0,
                SourceLocation.of(context.input().sourceName(), literal)));
    }

    private boolean isDeclaredConstantInitializer(Node literal) {
        Node current = literal;

        while (current != null) {
            if (current instanceof FieldDeclaration field) {
                return field.isFinal();
            }

            if (current instanceof VariableDeclarationExpr declaration) {
                return declaration.hasModifier(Modifier.Keyword.FINAL);
            }

            current = current.getParentNode().orElse(null);
        }

        return false;
    }

    private boolean isAllowedNumber(Node literal) {
        String value = numericText(literal);

        try {
            BigDecimal number = new BigDecimal(value);
            return number.compareTo(BigDecimal.ZERO) == 0
                    || number.compareTo(BigDecimal.ONE) == 0
                    || number.compareTo(BigDecimal.ONE.negate()) == 0
                    || number.compareTo(BigDecimal.valueOf(2)) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private double numericValue(Node literal) {
        try {
            return new BigDecimal(numericText(literal)).doubleValue();
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String numericText(Node literal) {
        String value = literal.toString()
                .replace("_", "")
                .replaceAll("[lLfFdD]$", "");

        if (literal.getParentNode().orElse(null) instanceof UnaryExpr unary
                && unary.getOperator() == UnaryExpr.Operator.MINUS) {
            value = "-" + value;
        }

        return value;
    }
}