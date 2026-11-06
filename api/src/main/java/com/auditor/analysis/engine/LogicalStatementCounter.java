package com.auditor.analysis.engine;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;

final class LogicalStatementCounter {

    private LogicalStatementCounter() {
    }

    static int count(CallableDeclaration<?> callable) {
        return callable.findAll(Statement.class).stream()
                .filter(statement -> isOwnedByCallable(statement, callable))
                .filter(statement ->
                        !(statement instanceof BlockStmt)
                                && !(statement instanceof EmptyStmt))
                .toList()
                .size();
    }

    private static boolean isOwnedByCallable(
            Statement statement,
            CallableDeclaration<?> callable) {

        Node current = statement;

        while (current != null && current != callable) {
            if (current instanceof LambdaExpr
                    || current instanceof TypeDeclaration<?>
                    || current instanceof ObjectCreationExpr creation
                            && creation.getAnonymousClassBody().isPresent()) {
                return false;
            }

            current = current.getParentNode().orElse(null);
        }

        return current == callable;
    }
}