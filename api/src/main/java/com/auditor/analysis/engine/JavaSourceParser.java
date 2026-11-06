package com.auditor.analysis.engine;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.TokenRange;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;

import java.util.List;

public class JavaSourceParser {

    public JavaParseResult parse(AnalysisInput input) {
        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(LanguageLevel.JAVA_21));
        ParseResult<CompilationUnit> result = parser.parse(input.source());
        List<SyntaxDiagnostic> diagnostics = result.getProblems().stream()
                .map(problem -> toDiagnostic(input.sourceName(), problem))
                .toList();
        return new JavaParseResult(result.getResult(), diagnostics);
    }

    private SyntaxDiagnostic toDiagnostic(String sourceName, Problem problem) {
        SourceLocation location = problem.getLocation()
                .map(tokenRange -> tokenLocation(sourceName, tokenRange))
                .orElseGet(() -> new SourceLocation(sourceName, 0, 0, 0, 0));
        return new SyntaxDiagnostic(problem.getMessage(), location);
    }

    private SourceLocation tokenLocation(String sourceName, TokenRange tokenRange) {
        Range beginRange = tokenRange.getBegin().getRange().orElse(null);
        Range endRange = tokenRange.getEnd().getRange().orElse(beginRange);
        if (beginRange == null || endRange == null) {
            return new SourceLocation(sourceName, 0, 0, 0, 0);
        }
        return new SourceLocation(
                sourceName,
                beginRange.begin.line,
                beginRange.begin.column,
                endRange.end.line,
                endRange.end.column);
    }
}