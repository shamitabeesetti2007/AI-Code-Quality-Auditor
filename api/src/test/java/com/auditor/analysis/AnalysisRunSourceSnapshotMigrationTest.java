package com.auditor.analysis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisRunSourceSnapshotMigrationTest {

    @Test
    void migrationLeavesHistoricalRunsUnattributed() throws IOException {
        String migration;
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V6__analysis_run_source_snapshot.sql")) {
            if (input == null) {
                throw new IOException("V6 migration resource was not found.");
            }
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("ADD COLUMN source_snapshot TEXT"));
        assertTrue(migration.contains("ADD COLUMN source_hash CHAR(64)"));
        assertTrue(migration.contains("source_snapshot IS NULL"));
        assertTrue(migration.contains("source_hash IS NULL"));
        assertFalse(migration.matches(
                "(?is).*ADD\\s+COLUMN\\s+source_snapshot\\s+TEXT\\s+NOT\\s+NULL.*"));
        assertFalse(migration.matches(
                "(?is).*ADD\\s+COLUMN\\s+source_hash\\s+CHAR\\(64\\)\\s+NOT\\s+NULL.*"));
        assertFalse(migration.matches("(?is).*UPDATE\\s+analysis_run\\b.*"));
    }
}
