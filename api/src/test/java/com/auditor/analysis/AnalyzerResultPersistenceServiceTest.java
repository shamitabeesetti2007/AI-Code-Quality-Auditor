package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.AnalyzerFinding;
import com.auditor.analysis.contract.AnalyzerMetric;
import com.auditor.analysis.contract.AnalyzerSeverity;
import com.auditor.analysis.contract.ParseOutcome;
import com.auditor.submission.CodeSubmission;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.SubmissionOriginType;
import com.auditor.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzerResultPersistenceServiceTest {

    private final AnalysisRunRepository runRepository =
            mock(AnalysisRunRepository.class);
    private final AnalysisMetricRepository metricRepository =
            mock(AnalysisMetricRepository.class);
    private final AnalysisFindingRepository findingRepository =
            mock(AnalysisFindingRepository.class);
    private final AnalyzerResultPersistenceService service =
            new AnalyzerResultPersistenceService(
                    runRepository, metricRepository, findingRepository);

    private AppUser owner;
    private AnalysisRun run;

    @BeforeEach
    void setUp() {
        owner = new AppUser("engine@example.com", "password-hash", "Owner");
        CodeSubmission submission = new CodeSubmission(
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
}
