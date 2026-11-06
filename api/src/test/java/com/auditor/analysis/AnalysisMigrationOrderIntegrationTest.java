package com.auditor.analysis;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AnalysisMigrationOrderIntegrationTest {
    @Autowired DataSource dataSource;

    @Test
    void allMigrationsApplyToAnIsolatedEmptySchema() throws Exception {
        String schema = "migration_check_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration").load().migrate();
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                try (ResultSet rows = statement.executeQuery(
                        "SELECT is_nullable FROM information_schema.columns "
                                + "WHERE table_schema = '" + schema + "' "
                                + "AND table_name = 'analysis_finding' AND column_name = 'severity'")) {
                    assertTrue(rows.next());
                    assertEquals("YES", rows.getString(1));
                }
                try (ResultSet rows = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + schema
                                + ".flyway_schema_history WHERE success AND version IS NOT NULL")) {
                    assertTrue(rows.next());
                    assertEquals(8, rows.getInt(1));
                }
                try (ResultSet rows = statement.executeQuery(
                        "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = '" + schema
                                + "' AND indexname = 'idx_analysis_run_submission_owner_created_run'")) {
                    assertTrue(rows.next());
                    assertEquals(1, rows.getInt(1));
                }
            }
        } finally {
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
