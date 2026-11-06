package com.auditor.analysis.engine;

public record SourceLocation(
        String sourceName,
        int startLine,
        int startColumn,
        int endLine,
        int endColumn) {

    public static SourceLocation of(String sourceName, com.github.javaparser.ast.Node node) {
        return node.getRange()
                .map(range -> new SourceLocation(
                        sourceName,
                        range.begin.line,
                        range.begin.column,
                        range.end.line,
                        range.end.column))
                .orElseGet(() -> new SourceLocation(sourceName, 0, 0, 0, 0));
    }
}