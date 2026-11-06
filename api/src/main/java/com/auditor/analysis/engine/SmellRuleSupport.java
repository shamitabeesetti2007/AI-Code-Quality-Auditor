package com.auditor.analysis.engine;

import java.util.List;

final class SmellRuleSupport {

    private SmellRuleSupport() {
    }

    static AnalyzerResult violation(
            String ruleId,
            String message,
            double measuredValue,
            double threshold,
            SourceLocation location) {

        Finding finding = new Finding(
                ruleId,
                message,
                measuredValue,
                threshold,
                location);

        return new AnalyzerResult(List.of(), List.of(finding));
    }

    static AnalyzerResult noViolation() {
        return AnalyzerResult.empty();
    }
}