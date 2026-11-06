package com.auditor.analysis.engine;

import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.CatchClause;

import java.util.ArrayList;
import java.util.List;

public class IgnoredExceptionRule implements SmellRule {

    public static final String RULE_ID = "MAINTAINABILITY_IGNORED_EXCEPTION";

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (CatchClause clause :
                context.compilationUnit().findAll(CatchClause.class)) {

            if (clause.getBody().getStatements().isEmpty()
                    || !catchesBroadException(clause)
                    || usesExceptionParameter(clause)) {
                continue;
            }

            findings.add(new Finding(
                    RULE_ID,
                    "Caught Exception or Throwable is not handled or used.",
                    1,
                    0,
                    SourceLocation.of(context.input().sourceName(), clause)));
        }

        return new AnalyzerResult(List.of(), findings);
    }

    private boolean catchesBroadException(CatchClause clause) {
        String[] caughtTypes = clause.getParameter()
                .getType()
                .toString()
                .replace(" ", "")
                .split("\\|");

        for (String type : caughtTypes) {
            if (type.equals("Exception")
                    || type.equals("java.lang.Exception")
                    || type.equals("Throwable")
                    || type.equals("java.lang.Throwable")) {
                return true;
            }
        }

        return false;
    }

    private boolean usesExceptionParameter(CatchClause clause) {
        String parameterName = clause.getParameter().getNameAsString();

        return clause.getBody().findAll(NameExpr.class).stream()
                .anyMatch(name -> name.getNameAsString().equals(parameterName));
    }
}