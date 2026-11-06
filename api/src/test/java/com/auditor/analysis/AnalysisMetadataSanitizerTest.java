package com.auditor.analysis;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AnalysisMetadataSanitizerTest {
    @Test
    void keepsKnownAnalysisFieldsAndDropsArbitrarySourceAndSecretMetadata() {
        Map<String, Object> metadata = AnalysisMetadataSanitizer.metric(Map.of(
                "analyzerId", "java-complexity",
                "sourceName", "C:/private/Example.java",
                "startLine", 3,
                "numericValue", new BigDecimal("12"),
                "sourceCode", "class SECRET {}",
                "apiKey", "private-token",
                "observations", List.of(Map.of(
                        "sourceName", "Example.java",
                        "numericValue", 12,
                        "source", "class SECRET {}"))));

        assertEquals("java-complexity", metadata.get("analyzerId"));
        assertEquals("Example.java", metadata.get("sourceName"));
        assertEquals(3, metadata.get("startLine"));
        assertFalse(metadata.containsKey("sourceCode"));
        assertFalse(metadata.containsKey("apiKey"));
        assertFalse(metadata.toString().contains("class SECRET"));
    }

    @Test
    void findingMetadataOnlyKeepsNumericScoringInputs() {
        Map<String, Object> metadata = AnalysisMetadataSanitizer.finding(Map.of(
                "measuredValue", 14,
                "threshold", 10,
                "debug", "internal details"));
        assertEquals(Map.of("measuredValue", 14, "threshold", 10), metadata);
    }
}
