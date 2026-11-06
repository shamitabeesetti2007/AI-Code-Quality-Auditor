package com.auditor.analysis;

import com.auditor.analysis.dto.AnalysisReportResponse;
import com.auditor.submission.CodeSubmission;
import com.auditor.submission.CodeSubmissionRepository;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.SubmissionOriginType;
import com.auditor.submission.SubmissionSourceProcessor;
import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AnalysisReportIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodeSubmissionRepository submissionRepository;

    @Autowired
    private AnalysisRunRepository runRepository;

    @Autowired
    private AnalysisMetricRepository metricRepository;

    @Autowired
    private AnalysisFindingRepository findingRepository;

    @Autowired
    private AnalysisRunService runService;

    @Test
    void reportIncludesPersistedMetricsFindingsAndScoreWithoutSource() {
        AppUser owner = createOwner();
        CodeSubmission submission = createSubmission(owner);
        AnalysisRun run = createRun(submission, owner);
        Instant startedAt = Instant.parse("2026-10-30T12:00:00Z");
        run.transitionTo(AnalysisRunStatus.PARSING, startedAt, null, null);
        run.transitionTo(
                AnalysisRunStatus.ANALYZING, startedAt.plusSeconds(1), null, null);
        run.transitionTo(
                AnalysisRunStatus.SCORING, startedAt.plusSeconds(2), null, null);
        run.assignScoreAndGrade(new BigDecimal("90.00"), "A");
        run.transitionTo(
                AnalysisRunStatus.COMPLETED, startedAt.plusSeconds(3), null, null);
        runRepository.saveAndFlush(run);

        metricRepository.save(new AnalysisMetric(
                run,
                "complexity",
                "cyclomatic_complexity",
                new BigDecimal("4"),
                Map.of("method", "analyze")));
        findingRepository.save(new AnalysisFinding(
                run,
                "RULE_TEST",
                "1".repeat(64),
                FindingSeverity.MEDIUM,
                "Example finding",
                "Persisted finding description",
                "Example.java",
                3,
                2,
                3,
                10,
                "CWE-000",
                Map.of("origin", "test")));

        AnalysisReportResponse report = runService.getReport(
                submission.getSubmissionId(), run.getRunId(), owner.getUserId());

        assertEquals(AnalysisRunStatus.COMPLETED, report.run().status());
        assertEquals(new BigDecimal("90.00"), report.run().score());
        assertEquals("A", report.run().grade());
        assertEquals(AnalysisReportResponse.DataAvailability.COMPLETE,
                report.dataAvailability());
        assertEquals(new BigDecimal("4"),
                report.metricsByDimension().get("complexity").getFirst().numericValue());
        assertEquals(1, report.findingsSummary().totalCount());
        assertEquals(1L,
                report.findingsSummary().countsBySeverity().get(FindingSeverity.MEDIUM));
        assertEquals("CWE-000", report.findings().getFirst().cweId());
        assertTrue(report.run().completedAt().isAfter(report.run().startedAt()));
        assertEquals(
                List.of("run", "dataAvailability", "metricsByDimension",
                        "findingsSummary", "findings"),
                Arrays.stream(AnalysisReportResponse.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
        assertEquals(
                List.of("runId", "submissionId", "status", "score", "grade",
                        "createdAt", "startedAt", "completedAt", "errorSummary"),
                Arrays.stream(report.run().getClass().getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
    }

    @Test
    void incompleteAndFailedRunsKeepResultsUnavailableAndErrorsSafe() {
        AppUser owner = createOwner();
        CodeSubmission submission = createSubmission(owner);
        AnalysisRun partial = createRun(submission, owner);
        partial.transitionTo(AnalysisRunStatus.PARSING, Instant.now(), null, null);
        partial.transitionTo(AnalysisRunStatus.ANALYZING, Instant.now(), null, null);
        partial.transitionTo(
                AnalysisRunStatus.PARTIAL,
                Instant.now(),
                null,
                "internal engine exception details");
        runRepository.saveAndFlush(partial);

        AnalysisRun failed = createRun(submission, owner);
        failed.transitionTo(
                AnalysisRunStatus.FAILED,
                Instant.now(),
                null,
                "internal failure details");
        runRepository.saveAndFlush(failed);

        AnalysisRun parseFailed = createRun(submission, owner);
        parseFailed.transitionTo(AnalysisRunStatus.PARSING, Instant.now(), null, null);
        parseFailed.transitionTo(
                AnalysisRunStatus.PARSE_FAILED,
                Instant.now(),
                null,
                "internal parser details");
        runRepository.saveAndFlush(parseFailed);

        AnalysisReportResponse partialReport = runService.getReport(
                submission.getSubmissionId(), partial.getRunId(), owner.getUserId());
        AnalysisReportResponse failedReport = runService.getReport(
                submission.getSubmissionId(), failed.getRunId(), owner.getUserId());
        AnalysisReportResponse parseFailedReport = runService.getReport(
                submission.getSubmissionId(), parseFailed.getRunId(), owner.getUserId());

        assertEquals(AnalysisReportResponse.DataAvailability.PARTIAL,
                partialReport.dataAvailability());
        assertEquals("Analysis completed with unavailable checks.",
                partialReport.run().errorSummary());
        assertNull(partialReport.run().score());
        assertNull(partialReport.run().grade());
        assertEquals(AnalysisReportResponse.DataAvailability.UNAVAILABLE,
                failedReport.dataAvailability());
        assertEquals("Analysis failed unexpectedly.", failedReport.run().errorSummary());
        assertEquals(AnalysisReportResponse.DataAvailability.UNAVAILABLE,
                parseFailedReport.dataAvailability());
        assertEquals("The Java source could not be parsed.",
                parseFailedReport.run().errorSummary());
        assertTrue(parseFailedReport.metricsByDimension().isEmpty());
        assertNull(parseFailedReport.findingsSummary());
        assertTrue(parseFailedReport.findings().isEmpty());
        assertNull(failedReport.findingsSummary());
    }

    @Test
    void reportCannotUseRunFromAnotherSubmission() {
        AppUser owner = createOwner();
        CodeSubmission requestedSubmission = createSubmission(owner);
        CodeSubmission runSubmission = createSubmission(owner);
        AnalysisRun run = runRepository.saveAndFlush(
                createRun(runSubmission, owner));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> runService.getReport(
                        requestedSubmission.getSubmissionId(),
                        run.getRunId(),
                        owner.getUserId()));

        assertEquals(404, exception.getStatusCode().value());
    }

    private AppUser createOwner() {
        return userRepository.saveAndFlush(new AppUser(
                "report-" + UUID.randomUUID() + "@example.com",
                "report-test-hash",
                "Report test"));
    }

    private CodeSubmission createSubmission(AppUser owner) {
        String source = "class REPORT_SOURCE_SECRET {}";
        SubmissionSourceProcessor.NormalizedSource normalized =
                new SubmissionSourceProcessor().process(source);
        return submissionRepository.saveAndFlush(new CodeSubmission(
                owner,
                "Report test",
                SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN,
                normalized.sourceCode(),
                normalized.contentHash(),
                normalized.logicalLoc()));
    }

    private AnalysisRun createRun(CodeSubmission submission, AppUser owner) {
        AnalysisRun run = new AnalysisRun(
                submission,
                owner,
                "class REPORT_SOURCE_SECRET {}",
                "0".repeat(64));
        ReflectionTestUtils.setField(
                run,
                "createdAt",
                Instant.parse("2026-10-30T11:59:59Z"));
        return run;
    }
}
