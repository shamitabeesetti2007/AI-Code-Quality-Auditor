package com.auditor.submission;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionSourceProcessorTest {

    private final SubmissionSourceProcessor processor =
            new SubmissionSourceProcessor();

    @Test
    void stripsBomAndNormalizesAllLineEndingsBeforeHashing() {
        var source = processor.process("\uFEFFclass A {}\r\n// comment\r");

        assertEquals("class A {}\n// comment\n", source.sourceCode());
        assertEquals(1, source.logicalLoc());
        assertEquals(
                processor.process("class A {}\n// comment\n").contentHash(),
                source.contentHash());
        assertTrue(source.toString().startsWith("NormalizedSource[contentHash="));
        assertNotEquals(source.sourceCode(), source.toString());
    }

    @Test
    void hashIsDeterministicForEquivalentNormalizedSource() {
        var first = processor.process("class A {}\n");
        var second = processor.process("class A {}\r\n");

        assertEquals(first.contentHash(), second.contentHash());
        assertEquals(64, first.contentHash().length());
    }

    @Test
    void rejectsSourceWhenNormalizedUtf8SizeExceedsLimit() {
        String source = "é".repeat(50_001);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> processor.process(source));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, exception.getStatusCode());
    }

    @Test
    void countsNonblankLinesAndSkipsObviousCommentOnlyLines() {
        String source = """
                class Example {
                    // a line comment
                    /* block comment
                     * continuation
                     */
                    int count; /* inline comment */
                }
                """;

        assertEquals(
                3,
                processor.process(source).logicalLoc());
    }

    @Test
    void rejectsUnsupportedLanguageAndClientRemediationProvenance() {
        ResponseStatusException language = assertThrows(
                ResponseStatusException.class,
                () -> processor.normalizeLanguage("PYTHON"));
        ResponseStatusException origin = assertThrows(
                ResponseStatusException.class,
                () -> processor.parseClientOrigin("AI_REMEDIATED"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, language.getStatusCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, origin.getStatusCode());
        assertEquals(
                SubmissionOriginType.AI_ASSISTED,
                processor.parseClientOrigin("ai_assisted"));
    }

    @Test
    void rejectsClearNonJavaButDoesNotRequireSuccessfulParsing() {
        ResponseStatusException nonJava = assertThrows(
                ResponseStatusException.class,
                () -> processor.process("def example():\n    pass\n"));

        assertEquals(
                HttpStatus.UNPROCESSABLE_ENTITY, nonJava.getStatusCode());
        assertEquals(
                "class ???",
                processor.process("class ???").sourceCode());
    }

    @Test
    void rejectsWhitespaceOnlySourceAfterNormalization() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> processor.process("\uFEFF \r\n\t"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
