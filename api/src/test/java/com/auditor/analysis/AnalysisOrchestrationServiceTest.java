package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.CodeAnalyzer;
import com.auditor.analysis.contract.ParseOutcome;
import com.auditor.submission.CodeSubmission;
import com.auditor.submission.CodeSubmissionRepository;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.SubmissionOriginType;
import com.auditor.submission.SubmissionSourceProcessor;
import com.auditor.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalysisOrchestrationServiceTest {

    private final AnalysisRunRepository runRepository =
            mock(AnalysisRunRepository.class);
    private final AnalysisMetricRepository metricRepository =
            mock(AnalysisMetricRepository.class);
    private final AnalysisFindingRepository findingRepository =
            mock(AnalysisFindingRepository.class);
    private final CodeSubmissionRepository submissionRepository =
            mock(CodeSubmissionRepository.class);
    private final CodeAnalyzer codeAnalyzer = mock(CodeAnalyzer.class);
    private final AnalysisRunService runService = new AnalysisRunService(
            runRepository,
            metricRepository,
            findingRepository,
            submissionRepository);
    private final AnalyzerResultPersistenceService persistenceService =
            new AnalyzerResultPersistenceService(
                    new AnalyzerResultPersistenceTransaction(
                            runRepository, metricRepository, findingRepository,
                            new AnalysisScoringService(new AnalysisScoringPolicy())),
                    runService);
    private final AnalysisOrchestrationService service =
            new AnalysisOrchestrationService(
                    runRepository,
                    runService,
                    codeAnalyzer,
                    persistenceService,
                    new SubmissionSourceProcessor());

    private AppUser owner;
    private CodeSubmission submission;
    private AnalysisRun run;

    @BeforeEach
    void setUp() {
        owner = new AppUser("orchestration@example.com", "password-hash", "Owner");
        submission = new CodeSubmission(
                owner,
                "Example",
                SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN,
                "class OriginalSubmission {}",
                "0".repeat(64),
                1);
        run = new AnalysisRun(
                submission,
                owner,
                "\uFEFFclass Snapshot {}\r\n",
                "1".repeat(64));
        when(runRepository.findByRunIdAndOwner_UserId(
                run.getRunId(), owner.getUserId()))
                .thenReturn(Optional.of(run));
        when(runRepository.save(any(AnalysisRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void analyzesNormalizedImmutableSnapshotAndPersistsSuccessfulResult() {
        AnalysisEngineResult result = result(
                ParseOutcome.SUCCEEDED,
                AnalyzerExecutionStatus.COMPLETED,
                null);
        when(codeAnalyzer.analyze("class Snapshot {}\n")).thenReturn(result);
        submission.updateContent(
                "Updated", "class CurrentSubmission {}", "2".repeat(64), 1);

        service.analyze(run.getRunId(), owner.getUserId());

        assertEquals(AnalysisRunStatus.COMPLETED, run.getStatus());
        assertEquals("\uFEFFclass Snapshot {}\r\n", run.getSourceSnapshot());
        verify(codeAnalyzer).analyze("class Snapshot {}\n");
        verify(runRepository, org.mockito.Mockito.times(2)).save(run);
    }

    @Test
    void persistsParseFailureResultAndTransitionsRun() {
        AnalysisEngineResult result = result(
                ParseOutcome.FAILED,
                AnalyzerExecutionStatus.PARSE_FAILED,
                "Unable to parse source.");
        when(codeAnalyzer.analyze("class Snapshot {}\n")).thenReturn(result);

        service.analyze(run.getRunId(), owner.getUserId());

        assertEquals(AnalysisRunStatus.PARSE_FAILED, run.getStatus());
        assertEquals("The Java source could not be parsed.", run.getErrorSummary());
        assertNull(run.getScore());
        assertNull(run.getGrade());
        verify(codeAnalyzer).analyze("class Snapshot {}\n");
        verify(runRepository, org.mockito.Mockito.times(2)).save(run);
    }

    @Test
    void persistsPartialAnalysisResultAndTransitionsRun() {
        AnalysisEngineResult result = result(
                ParseOutcome.SUCCEEDED,
                AnalyzerExecutionStatus.PARTIAL,
                "Some checks were unavailable.");
        when(codeAnalyzer.analyze("class Snapshot {}\n")).thenReturn(result);

        service.analyze(run.getRunId(), owner.getUserId());

        assertEquals(AnalysisRunStatus.PARTIAL, run.getStatus());
        assertEquals("Analysis completed with unavailable checks.", run.getErrorSummary());
        assertNull(run.getScore());
        assertNull(run.getGrade());
        verify(runRepository, org.mockito.Mockito.times(2)).save(run);
    }

    @Test
    void engineExceptionFailsRunWithSafeSummaryAndSurfacesServerError() {
        when(codeAnalyzer.analyze("class Snapshot {}\n"))
                .thenThrow(new IllegalStateException("sensitive engine detail"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.analyze(run.getRunId(), owner.getUserId()));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals(AnalysisRunStatus.FAILED, run.getStatus());
        assertEquals(
                "Analysis engine failed unexpectedly.", run.getErrorSummary());
        verify(runRepository, org.mockito.Mockito.times(2))
                .save(any(AnalysisRun.class));
        verify(metricRepository, never()).save(any(AnalysisMetric.class));
        verify(findingRepository, never()).save(any(AnalysisFinding.class));
    }

    @Test
    void missingOrUnownedRunIsNotFoundAndNeverInvokesEngine() {
        UUID anotherOwner = UUID.randomUUID();
        when(runRepository.findByRunIdAndOwner_UserId(
                run.getRunId(), anotherOwner))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.analyze(run.getRunId(), anotherOwner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(codeAnalyzer);
    }

    @Test
    void repeatedInvocationOfNonQueuedRunIsRejected() {
        run.transitionTo(
                AnalysisRunStatus.PARSING,
                java.time.Instant.now(),
                null,
                null);

        assertThrows(
                IllegalStateException.class,
                () -> service.analyze(run.getRunId(), owner.getUserId()));

        verifyNoInteractions(codeAnalyzer);
        verify(runRepository, never()).save(any(AnalysisRun.class));
    }

    @Test
    void alreadyTerminalRunCannotBeAnalyzedAgain() {
        java.time.Instant transitionedAt = java.time.Instant.now();
        run.transitionTo(
                AnalysisRunStatus.PARSING, transitionedAt, null, null);
        run.transitionTo(
                AnalysisRunStatus.ANALYZING, transitionedAt, null, null);
        run.transitionTo(
                AnalysisRunStatus.PARTIAL, transitionedAt, null, null);

        assertThrows(
                IllegalStateException.class,
                () -> service.analyze(run.getRunId(), owner.getUserId()));

        verifyNoInteractions(codeAnalyzer);
        verify(runRepository, never()).save(any(AnalysisRun.class));
    }

    private static AnalysisEngineResult result(
            ParseOutcome parseOutcome,
            AnalyzerExecutionStatus executionStatus,
            String errorSummary) {
        return new AnalysisEngineResult(
                parseOutcome,
                "test-engine",
                executionStatus,
                errorSummary,
                List.of(),
                List.of());
    }
}
