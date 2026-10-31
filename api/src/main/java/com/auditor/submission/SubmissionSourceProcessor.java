package com.auditor.submission;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SubmissionSourceProcessor {

    public static final int MAX_SOURCE_BYTES = 100_000;

    private static final Pattern OBVIOUS_NON_JAVA = Pattern.compile(
            "(?m)^\\s*(?:"
                    + "def\\s+\\w+\\s*\\([^)]*\\)\\s*:"
                    + "|class\\s+\\w+\\s*:"
                    + "|(?:const|let|function)\\s+\\w+"
                    + "|#include\\s*[<\"]"
                    + "|using\\s+System\\s*;"
                    + "|fun\\s+\\w+\\s*\\("
                    + "|package\\s+main\\s*$"
                    + "|<\\?php"
                    + ")",
            Pattern.MULTILINE);

    public NormalizedSource process(String source) {
        if (source == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Source code is required.");
        }

        String normalized = source.startsWith("\uFEFF")
                ? source.substring(1)
                : source;
        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n');

        if (normalized.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Source code must not be blank.");
        }
        if (normalized.getBytes(StandardCharsets.UTF_8).length
                > MAX_SOURCE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.CONTENT_TOO_LARGE,
                    "Source code exceeds the 100,000-byte limit.");
        }
        if (OBVIOUS_NON_JAVA.matcher(normalized).find()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Source code does not appear to be Java.");
        }
        return new NormalizedSource(
                normalized,
                sha256(normalized),
                calculateLogicalLoc(normalized));
    }

    public String normalizeLanguage(String language) {
        if (language == null || !"JAVA".equalsIgnoreCase(language.strip())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unsupported language. JAVA is currently supported.");
        }
        return SubmissionLanguage.JAVA.name();
    }

    public SubmissionOriginType parseClientOrigin(String originType) {
        final SubmissionOriginType origin;
        try {
            origin = SubmissionOriginType.valueOf(
                    originType.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unsupported submission origin type.");
        }
        if (origin == SubmissionOriginType.AI_REMEDIATED) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_REMEDIATED provenance is reserved for trusted workflows.");
        }
        return origin;
    }

    /**
     * Counts nonblank lines after skipping obvious line and block comments.
     * This is intentionally a language-light approximation: comment markers
     * inside literals, inline code comments, and language-specific syntax are
     * not parsed and can affect the count.
     */
    static int calculateLogicalLoc(String source) {
        int lines = 0;
        boolean insideBlockComment = false;
        for (String line : source.split("\n", -1)) {
            String remaining = line.strip();
            if (remaining.isEmpty()) {
                continue;
            }

            while (!remaining.isEmpty()) {
                if (insideBlockComment) {
                    int close = remaining.indexOf("*/");
                    if (close < 0) {
                        remaining = "";
                    } else {
                        remaining = remaining.substring(close + 2).strip();
                        insideBlockComment = false;
                    }
                } else if (remaining.startsWith("//")) {
                    remaining = "";
                } else if (remaining.startsWith("/*")) {
                    int close = remaining.indexOf("*/", 2);
                    if (close < 0) {
                        insideBlockComment = true;
                        remaining = "";
                    } else {
                        remaining = remaining.substring(close + 2).strip();
                    }
                } else {
                    lines++;
                    remaining = "";
                }
            }
        }
        return lines;
    }

    private static String sha256(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    public record NormalizedSource(
            String sourceCode,
            String contentHash,
            int logicalLoc) {

        @Override
        public String toString() {
            return "NormalizedSource[contentHash=" + contentHash
                    + ", logicalLoc=" + logicalLoc + "]";
        }
    }
}
