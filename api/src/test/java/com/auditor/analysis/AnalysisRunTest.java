package com.auditor.analysis;

import com.auditor.submission.CodeSubmission;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.SubmissionOriginType;
import com.auditor.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisRunTest {

    @Test
    void allowsExpectedProgressionAndMarksTerminalTimestamps() {
        AnalysisRun run = queuedRun();
        Instant now = Instant.now();

        run.transitionTo(AnalysisRunStatus.PARSING, now, "v1", null);
        run.transitionTo(AnalysisRunStatus.ANALYZING, now, "v1", null);
        run.transitionTo(AnalysisRunStatus.SCORING, now, "v1", null);
        run.transitionTo(AnalysisRunStatus.COMPLETED, now, "v1", null);

        assertEquals(AnalysisRunStatus.COMPLETED, run.getStatus());
        assertEquals(now, run.getStartedAt());
        assertEquals(now, run.getCompletedAt());
        assertNotNull(run.getAnalyzerVersion());
    }

    @Test
    void rejectsInvalidTransitionsAndTransitionsAfterTerminalStates() {
        AnalysisRun run = queuedRun();

        assertThrows(
                IllegalStateException.class,
                () -> run.transitionTo(
                        AnalysisRunStatus.COMPLETED,
                        Instant.now(),
                        "v1",
                        null));

        run.transitionTo(
                AnalysisRunStatus.FAILED, Instant.now(), "v1", "failure");
        assertThrows(
                IllegalStateException.class,
                () -> run.transitionTo(
                        AnalysisRunStatus.PARSING,
                        Instant.now(),
                        "v1",
                        null));
    }

    private static AnalysisRun queuedRun() {
        AppUser owner = new AppUser(
                "run@example.com", "password-hash", "Run owner");
        CodeSubmission submission = new CodeSubmission(
                owner,
                "Run",
                SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN,
                "class Run {}",
                "0".repeat(64),
                1);
        return new AnalysisRun(
                submission, owner, "class Run {}", "0".repeat(64));
    }
}
