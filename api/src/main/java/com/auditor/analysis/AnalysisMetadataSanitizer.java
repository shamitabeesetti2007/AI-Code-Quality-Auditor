package com.auditor.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Keeps only metadata fields defined by the API's analyzer integration. */
public final class AnalysisMetadataSanitizer {
    private static final Set<String> METRIC_FIELDS = Set.of(
            "analyzerId", "sourceName", "startLine", "startColumn", "endLine",
            "endColumn", "aggregation", "observations", "numericValue");
    private static final Set<String> FINDING_FIELDS = Set.of("measuredValue", "threshold");

    private AnalysisMetadataSanitizer() {}

    public static Map<String, Object> metric(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source == null) return Collections.unmodifiableMap(result);
        for (String key : METRIC_FIELDS) {
            Object value = source.get(key);
            if (value instanceof Number && isNumericField(key)) result.put(key, value);
            else if ("analyzerId".equals(key) && safeIdentifier(value)) result.put(key, value);
            else if ("sourceName".equals(key) && safeFileName(value)) {
                String path = (String) value;
                result.put(key, path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1));
            }
            else if (isPosition(key) && value instanceof Number number
                    && number.longValue() > 0 && number.longValue() <= Integer.MAX_VALUE) {
                result.put(key, number.intValue());
            } else if ("aggregation".equals(key) && "maximum".equals(value)) {
                result.put(key, value);
            } else if ("observations".equals(key) && value instanceof Iterable<?> values) {
                ArrayList<Map<String, Object>> observations = new ArrayList<>();
                for (Object item : values) {
                    if (item instanceof Map<?, ?> raw) {
                        Map<String, Object> observation = new LinkedHashMap<>();
                        raw.forEach((k, v) -> {
                            if (k instanceof String s) observation.put(s, v);
                        });
                        observations.add(metric(observation));
                    }
                }
                result.put(key, List.copyOf(observations));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> finding(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source != null) for (String key : FINDING_FIELDS) {
            Object value = source.get(key);
            if (value instanceof Number) result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }

    private static boolean isNumericField(String key) {
        return "numericValue".equals(key);
    }

    private static boolean isPosition(String key) {
        return Set.of("startLine", "startColumn", "endLine", "endColumn").contains(key);
    }

    private static boolean safeIdentifier(Object value) {
        return value instanceof String s && s.matches("[A-Za-z0-9._-]{1,80}");
    }

    private static boolean safeFileName(Object value) {
        return value instanceof String s && s.length() <= 1024
                && !s.contains("\n") && !s.contains("\r")
                && s.substring(Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\')) + 1)
                        .matches("[A-Za-z0-9._ -]{1,255}")
                && !s.equals(".") && !s.equals("..");
    }
}
