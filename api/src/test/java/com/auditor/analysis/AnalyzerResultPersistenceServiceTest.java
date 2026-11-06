package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.AnalyzerFinding;
import com.auditor.analysis.contract.AnalyzerMetric;
import com.auditor.analysis.contract.AnalyzerSeverity;
import com.auditor.analysis.contract.ParseOutcome;
import com.auditor.submission.CodeSubmission;
import com.auditor.submission.CodeSubmissionRepository;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.SubmissionOriginType;
import com.auditor.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class AnalyzerResultPersistenceServiceTest {

    private final AnalysisRunRepository runRepository =
            mock(AnalysisRunRepository.class);
    private final AnalysisMetricRepository metricRepository =
            mock(AnalysisMetricRepository.class);
    private final AnalysisFindingRepository findingRepository =
            mock(AnalysisFindingRepository.class);
    private final CodeSubmissionRepository submissionRepository =
            mock(CodeSubmissionRepository.class);
    private final AnalysisRunService runService = new AnalysisRunService(
            runRepository, metricRepository, findingRepository, submissionRepository);
    private final AnalyzerResultPersistenceService service =
            new AnalyzerResultPersistenceService(
                    new AnalyzerResultPersistenceTransaction(
                            runRepository, metricRepository, findingRepository,
                            new AnalysisScoringService(new AnalysisScoringPolicy())),
                    runService);

    private AppUser owner;
    private CodeSubmission submission;
    private AnalysisRun run;

    @BeforeEach
    void setUp() {
        owner = new AppUser("engine@example.com", "password-hash", "Owner");
        submission = new CodeSubmission(
                owner,
                "Example",
                SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN,
                "class Example {}",
                "0".repeat(64),
                1);
        run = new AnalysisRun(
                submission,
                owner,
                "class Example {}",
                "0".repeat(64));
        when(runRepository.findByRunIdAndOwner_UserId(
                run.getRunId(), owner.getUserId()))
                .thenReturn(Optional.of(run));
        when(runRepository.save(any(AnalysisRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void mapsIndependentAnalyzerResultAndCompletesThePersistedRun() {
        AnalysisEngineResult result = new AnalysisEngineResult(
                ParseOutcome.SUCCEEDED,
                "engine-1.0",
                AnalyzerExecutionStatus.COMPLETED,
                null,
                List.of(new AnalyzerFinding(
                        "RULE-1",
                        "a".repeat(64),
                        AnalyzerSeverity.HIGH,
                        "Finding message",
                        "Finding details",
                        "src/Example.java",
                        3,
                        2,
                        3,
                        10,
                        "CWE-79",
                        Map.of("source", "test"))),
                List.of(new AnalyzerMetric(
                        "quality",
                        "example",
                        new BigDecimal("1.5"),
                        Map.of("unit", "points"))));

        service.persist(run.getRunId(), owner.getUserId(), result);

        assertEquals(AnalysisRunStatus.COMPLETED, run.getStatus());
        assertEquals("engine-1.0", run.getAnalyzerVersion());
        assertNotNull(run.getStartedAt());
        assertNotNull(run.getCompletedAt());
        verify(metricRepository).save(any(AnalysisMetric.class));
        verify(findingRepository).save(any(AnalysisFinding.class));
        verify(runRepository).save(run);
    }

    @Test
    void persistsFindingWithUnavailableSeverityAsNull() {
        AnalysisEngineResult result = new AnalysisEngineResult(
                ParseOutcome.SUCCEEDED,
                "engine-1.0",
                AnalyzerExecutionStatus.COMPLETED,
                null,
                List.of(new AnalyzerFinding(
                        "RULE-1",
                        "b".repeat(64),
                        null,
                        "Finding message",
                        null,
                        "Source.java",
                        1,
                        1,
                        1,
                        10,
                        null,
                        Map.of())),
                List.of());

        service.persist(run.getRunId(), owner.getUserId(), result);

        ArgumentCaptor<AnalysisFinding> captor =
                ArgumentCaptor.forClass(AnalysisFinding.class);
        verify(findingRepository).save(captor.capture());
        assertNull(captor.getValue().getSeverity());
    }

    @Test
    void scoresCompletedRunAndPersistsUniqueMetricKeys() {
        AnalysisEngineResult result = new AnalysisEngineResult(
                ParseOutcome.SUCCEEDED,
                "engine-1.0",
                AnalyzerExecutionStatus.COMPLETED,
                null,
                List.of(),
                List.of(
                        metric("cyclomatic.complexity", "11", 1),
                        metric("cyclomatic.complexity", "12", 10),
                        metric("cognitive.complexity", "16", 1),
                        metric("cognitive.complexity", "17", 10)));

        service.persist(run.getRunId(), owner.getUserId(), result);

        assertEquals(AnalysisRunStatus.COMPLETED, run.getStatus());
        assertEquals(new BigDecimal("91.00"), run.getScore());
        assertEquals("A", run.getGrade());
        verify(metricRepository, times(2)).save(any(AnalysisMetric.class));
        ArgumentCaptor<AnalysisMetric> captor =
                ArgumentCaptor.forClass(AnalysisMetric.class);
        verify(metricRepository, times(2)).save(captor.capture());
        assertEquals(2, captor.getAllValues().getFirst()
                .getMetadata().get("observations") instanceof List<?> observations
                ? observations.size()
                : 0);
    }

    @Test
    void scoringFailureTransitionsRunToFailedWithoutLeakingDetails() {
        AnalysisScoringService scoringService = mock(AnalysisScoringService.class);
        when(scoringService.score(any(AnalysisEngineResult.class)))
                .thenThrow(new IllegalStateException("private scoring details"));
        AnalyzerResultPersistenceService failingService =
                new AnalyzerResultPersistenceService(
                        new AnalyzerResultPersistenceTransaction(
                                runRepository, metricRepository, findingRepository,
                                scoringService),
                        runService);
        AnalysisEngineResult result = new AnalysisEngineResult(
                ParseOutcome.SUCCEEDED,
                "engine-1.0",
                AnalyzerExecutionStatus.COMPLETED,
                null,
                List.of(),
                List.of(
                        metric("cyclomatic.complexity", "1", 1),
                        metric("cognitive.complexity", "1", 1)));

        failingService.persist(run.getRunId(), owner.getUserId(), result);

        assertEquals(AnalysisRunStatus.FAILED, run.getStatus());
        assertEquals("Analysis scoring failed.", run.getErrorSummary());
        assertNull(run.getScore());
        assertNull(run.getGrade());
        verify(runRepository).save(run);
    }

    @Test
    void partialExecutionDoesNotReceiveScoreOrGrade() {
        AnalysisEngineResult result = new AnalysisEngineResult(
                ParseOutcome.SUCCEEDED,
                "engine-1.0",
                AnalyzerExecutionStatus.PARTIAL,
                "One analyzer was unavailable.",
                List.of(),
                List.of(
                        metric("cyclomatic.complexity", "1", 1),
                        metric("cognitive.complexity", "1", 1)));

        service.persist(run.getRunId(), owner.getUserId(), result);

        assertEquals(AnalysisRunStatus.PARTIAL, run.getStatus());
        assertNull(run.getScore());
        assertNull(run.getGrade());
    }

    @Test
    void notAttemptedFailedResultTransitionsToFailedSafely() {
        service.persist(run.getRunId(), owner.getUserId(), new AnalysisEngineResult(
                ParseOutcome.NOT_ATTEMPTED, "engine", AnalyzerExecutionStatus.FAILED,
                "private details", List.of(), List.of()));
        assertEquals(AnalysisRunStatus.FAILED, run.getStatus());
        assertEquals("Analysis failed unexpectedly.", run.getErrorSummary());
    }

    @Test
    void inconsistentResultAndNullMetricBecomeSafeTerminalFailures() {
        assertThrows(ResponseStatusException.class, () -> service.persist(
                run.getRunId(), owner.getUserId(), new AnalysisEngineResult(
                        ParseOutcome.NOT_ATTEMPTED, "engine", AnalyzerExecutionStatus.COMPLETED,
                        null, List.of(), List.of())));
        assertEquals(AnalysisRunStatus.FAILED, run.getStatus());
        assertEquals("Analysis failed unexpectedly.", run.getErrorSummary());

        run = new AnalysisRun(submission, owner, "class Example {}", "1".repeat(64));
        when(runRepository.findByRunIdAndOwner_UserId(run.getRunId(), owner.getUserId()))
                .thenReturn(Optional.of(run));
        AnalyzerMetric nullValueMetric = new AnalyzerMetric("quality", "bad", null, Map.of());
        assertThrows(ResponseStatusException.class, () -> service.persist(
                run.getRunId(), owner.getUserId(), new AnalysisEngineResult(
                        ParseOutcome.SUCCEEDED, "engine", AnalyzerExecutionStatus.COMPLETED,
                        null, List.of(), List.of(nullValueMetric))));
        assertEquals(AnalysisRunStatus.FAILED, run.getStatus());
        assertEquals("Analysis failed unexpectedly.", run.getErrorSummary());
    }

    @Test
    void persistenceExceptionFailsRunWithoutReturningDatabaseDetails() {
        AnalysisEngineResult result = new AnalysisEngineResult(
                ParseOutcome.SUCCEEDED, "engine", AnalyzerExecutionStatus.COMPLETED,
                null, List.of(), List.of(
                        metric("cyclomatic.complexity", "1", 1),
                        metric("cognitive.complexity", "1", 1)));
        doThrow(new IllegalStateException("secret database detail"))
                .when(metricRepository).save(any(AnalysisMetric.class));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.persist(run.getRunId(), owner.getUserId(), result));
        assertEquals(500, exception.getStatusCode().value());
        assertEquals(AnalysisRunStatus.FAILED, run.getStatus());
        assertEquals("Analysis failed unexpectedly.", run.getErrorSummary());
        assertFalse(exception.getReason().contains("secret database detail"));
    }

    private static AnalyzerMetric metric(String key, String value, int startLine) {
        return new AnalyzerMetric(
                "java-complexity",
                key,
                new BigDecimal(value),
                Map.of(
                        "sourceName", "Source.java",
                        "startLine", startLine,
                        "startColumn", 1,
                        "endLine", startLine,
                        "endColumn", 5));
    }
}
