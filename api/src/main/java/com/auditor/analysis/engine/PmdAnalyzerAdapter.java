
package com.auditor.analysis.engine;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

import java.util.ArrayList;
import java.util.List;

public class PmdAnalyzerAdapter implements Analyzer {

    private static final String RULESET =
            "rulesets/java/quickstart.xml";

    @Override
    public String analyzerId() {
        return "pmd";
    }

    @Override
    public AnalyzerResult analyze(AnalysisContext context) {

        List<Finding> findings = new ArrayList<>();

        PMDConfiguration configuration = new PMDConfiguration();

        configuration.setDefaultLanguageVersion(
                net.sourceforge.pmd.lang.java.JavaLanguageModule
                        .getInstance()
                        .getVersion("21"));

        try (PmdAnalysis pmd = PmdAnalysis.create(configuration)) {

            RuleSet ruleSet = pmd.newRuleSetLoader()
                    .loadFromResource(RULESET);

            pmd.addRuleSet(ruleSet);

            FileId fileId = FileId.fromPathLikeString(
                    context.input().sourceName());

            boolean added = pmd.files().addSourceFile(
                    fileId,
                    context.input().source());

            if (!added) {
                throw new IllegalStateException(
                        "Could not add Java source to PMD analysis.");
            }

            Report report = pmd.performAnalysisAndCollectReport();

            for (RuleViolation violation : report.getViolations()) {

                int startLine = violation.getBeginLine();
                int startColumn = violation.getBeginColumn();

                SourceLocation location = new SourceLocation(
                        context.input().sourceName(),
                        startLine,
                        startColumn,
                        violation.getEndLine(),
                        violation.getEndColumn());

                findings.add(new Finding(
                        violation.getRule().getName(),
                        violation.getDescription(),
                        0,
                        0,
                        location,
                        "PMD"));
            }

            if (!report.getProcessingErrors().isEmpty()) {
                throw new IllegalStateException(
                        "PMD reported processing errors: "
                                + report.getProcessingErrors());
            }

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "PMD analysis failed for "
                            + context.input().sourceName(),
                    exception);
        }

        return new AnalyzerResult(List.of(), findings);
    }
}
