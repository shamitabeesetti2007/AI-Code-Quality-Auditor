package com.auditor.analysis.engine;

import com.github.javaparser.ast.stmt.CatchClause;

import java.util.ArrayList;
import java.util.List;

public class EmptyCatchRule implements SmellRule {

    public static final String RULE_ID = "MAINTAINABILITY_EMPTY_CATCH";

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (CatchClause clause :
                context.compilationUnit().findAll(CatchClause.class)) {

            if (clause.getBody().getStatements().isEmpty()) {
                findings.add(new Finding(
                        RULE_ID,
                        "Empty catch block silently ignores an exception.",
                        0,
                        1,
                        SourceLocation.of(context.input().sourceName(), clause)));
            }
        }

        return new AnalyzerResult(List.of(), findings);
    }
}