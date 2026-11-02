package com.auditor.analysis;

import com.auditor.analysis.dto.AnalysisRunHistoryItemResponse;
import com.auditor.analysis.dto.AnalysisRunHistoryResponse;
import com.auditor.analysis.dto.AnalysisReportResponse;
import com.auditor.analysis.dto.AnalysisReportRunOverview;
import com.auditor.analysis.dto.AnalysisMetricResponse;
import com.auditor.submission.CodeSubmission;
import com.auditor.submission.CodeSubmissionRepository;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.SubmissionOriginType;
import com.auditor.user.AppUser;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class AnalysisRunServiceTest {

    private final AnalysisRunRepository runRepository =
            mock(AnalysisRunRepository.class);
    private final AnalysisMetricRepository metricRepository =
            mock(AnalysisMetricRepository.class);
    private final AnalysisFindingRepository findingRepository =
            mock(AnalysisFindingRepository.class);
    private final CodeSubmissionRepository submissionRepository =
            mock(CodeSubmissionRepository.class);
    private final AnalysisRunService service = new AnalysisRunService(
            runRepository,
            metricRepository,
            findingRepository,
            submissionRepository);

    private AppUser owner;
    private CodeSubmission submission;

    @BeforeEach
    void setUp() {
        owner = new AppUser("analysis@example.com", "password-hash", "Owner");
        submission = new CodeSubmission(
                owner,
                "Example",
                SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN,
                "class Example {}",
                "0".repeat(64),
                1);
        when(submissionRepository.findBySubmissionIdAndOwner_UserId(
                submission.getSubmissionId(), owner.getUserId()))
                .thenReturn(Optional.of(submission));
        when(runRepository.saveAndFlush(any(AnalysisRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void manualRunCreationPersistsQueuedStateForOwnedSubmission() {
        var response = service.createQueuedRun(
                submission.getSubmissionId(), owner.getUserId());

        assertEquals(AnalysisRunStatus.QUEUED, response.status());
        assertEquals("0".repeat(64), response.sourceHash());
        verify(runRepository).saveAndFlush(any(AnalysisRun.class));
    }

    @Test
    void eachRunCapturesTheSubmissionVersionAtCreation() {
        service.createQueuedRun(submission.getSubmissionId(), owner.getUserId());
        submission.updateContent(
                "Example v2",
                "class Example { int value; }",
                "1".repeat(64),
                1);
        service.createQueuedRun(submission.getSubmissionId(), owner.getUserId());

        ArgumentCaptor<AnalysisRun> captor =
                ArgumentCaptor.forClass(AnalysisRun.class);
        verify(runRepository, org.mockito.Mockito.times(2))
                .saveAndFlush(captor.capture());
        List<AnalysisRun> runs = captor.getAllValues();

        assertEquals("class Example {}", runs.get(0).getSourceSnapshot());
        assertEquals("0".repeat(64), runs.get(0).getSourceHash());
        assertEquals(
                "class Example { int value; }",
                runs.get(1).getSourceSnapshot());
        assertEquals("1".repeat(64), runs.get(1).getSourceHash());
    }

    @Test
    void anotherUsersSubmissionCannotBeUsedToCreateARun() {
        UUID otherOwnerId = UUID.randomUUID();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createQueuedRun(
                        submission.getSubmissionId(), otherOwnerId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(runRepository, never()).saveAndFlush(any(AnalysisRun.class));
    }

    @Test
    void runDetailUsesSafeStatusBasedErrorSummary() {
        AnalysisRun failed = new AnalysisRun(
                submission, owner, "class Example {}", "0".repeat(64));
        failed.transitionTo(AnalysisRunStatus.FAILED, Instant.now(), null,
                "parser token contained source and private diagnostics");
        when(runRepository.findByRunIdAndOwner_UserId(failed.getRunId(), owner.getUserId()))
                .thenReturn(Optional.of(failed));
        when(metricRepository.findAllByRun_RunIdAndRun_Owner_UserId(
                failed.getRunId(), owner.getUserId())).thenReturn(List.of());
        when(findingRepository.findAllByRun_RunIdAndRun_Owner_UserId(
                failed.getRunId(), owner.getUserId())).thenReturn(List.of());

        var response = service.getRun(failed.getRunId(), owner.getUserId());

        assertEquals("Analysis failed unexpectedly.", response.run().errorSummary());
        org.junit.jupiter.api.Assertions.assertFalse(
                response.toString().contains("private diagnostics"));
    }

    @Test
    void historyRequiresSubmissionOwnershipBeforeQueryingRuns() {
        UUID anotherOwnerId = UUID.randomUUID();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.listRuns(
                        submission.getSubmissionId(), anotherOwnerId, 0, 20));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(runRepository, never()).findHistoryBySubmissionAndOwner(
                any(UUID.class), any(UUID.class), any(Pageable.class));
    }

    @Test
    void historyReturnsPaginatedSummaryItemsWithoutSource() throws Exception {
        AnalysisRunHistoryItemResponse newest = historyItem(
                UUID.randomUUID(),
                AnalysisRunStatus.COMPLETED,
                new BigDecimal("95.00"),
                "A",
                Instant.parse("2026-10-30T12:00:00Z"),
                null);
        AnalysisRunHistoryItemResponse older = historyItem(
                UUID.randomUUID(),
                AnalysisRunStatus.FAILED,
                null,
                null,
                Instant.parse("2026-10-29T12:00:00Z"),
                "sensitive exception details");
        when(runRepository.findHistoryBySubmissionAndOwner(
                eq(submission.getSubmissionId()),
                eq(owner.getUserId()),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(newest, older), PageRequest.of(0, 20), 2));

        AnalysisRunHistoryResponse response = service.listRuns(
                submission.getSubmissionId(), owner.getUserId(), 0, 20);

        assertEquals(List.of(newest.runId(), older.runId()),
                response.items().stream()
                        .map(AnalysisRunHistoryItemResponse::runId)
                        .toList());
        assertEquals(AnalysisRunStatus.COMPLETED, response.items().getFirst().status());
        assertEquals(new BigDecimal("95.00"), response.items().getFirst().score());
        assertEquals("A", response.items().getFirst().grade());
        assertEquals(
                "Analysis failed unexpectedly.",
                response.items().get(1).errorSummary());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(2, response.totalItems());
        assertEquals(
                List.of("runId", "submissionId", "status", "score", "grade",
                        "createdAt", "startedAt", "completedAt", "errorSummary"),
                Arrays.stream(AnalysisRunHistoryItemResponse.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
    }

    @Test
    void historyUsesDatabasePageAndReportsEmptyPage() {
        when(runRepository.findHistoryBySubmissionAndOwner(
                eq(submission.getSubmissionId()),
                eq(owner.getUserId()),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        AnalysisRunHistoryResponse response = service.listRuns(
                submission.getSubmissionId(), owner.getUserId(), 2, 5);

        assertEquals(List.of(), response.items());
        assertEquals(2, response.page());
        assertEquals(5, response.size());
        assertEquals(0, response.totalItems());
        assertEquals(0, response.totalPages());
    }

    @Test
    void reportIncludesMetricsFindingsAndCountsUnclassifiedSeverity() {
        UUID runId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-30T12:00:00Z");
        AnalysisReportRunOverview overview = new AnalysisReportRunOverview(
                runId,
                submission.getSubmissionId(),
                AnalysisRunStatus.COMPLETED,
                new BigDecimal("91.00"),
                "A",
                createdAt,
                createdAt.plusSeconds(1),
                createdAt.plusSeconds(10),
                "private detail must not be forwarded");
        AnalysisRun run = new AnalysisRun(
                submission, owner, "class Example {}", "0".repeat(64));
        AnalysisMetric metric = new AnalysisMetric(
                run,
                "complexity",
                "cyclomatic_complexity",
                new BigDecimal("3"),
                java.util.Map.of("method", "run"));
        AnalysisFinding highFinding = new AnalysisFinding(
                run, "RULE_HIGH", "1".repeat(64), FindingSeverity.HIGH,
                "High issue", "Description", "Example.java",
                2, 1, 2, 8, null, java.util.Map.of("key", "value"));
        AnalysisFinding unknownFinding = new AnalysisFinding(
                run, "RULE_UNKNOWN", "2".repeat(64), null,
                "Unclassified issue", null, null,
                null, null, null, null, null, java.util.Map.of());
        when(submissionRepository.existsBySubmissionIdAndOwner_UserId(
                submission.getSubmissionId(), owner.getUserId()))
                .thenReturn(true);
        when(runRepository.findReportOverview(
                runId, submission.getSubmissionId(), owner.getUserId()))
                .thenReturn(Optional.of(overview));
        when(metricRepository
                .findAllByRun_RunIdAndRun_Owner_UserIdOrderByDimensionAscMetricKeyAsc(
                        runId, owner.getUserId()))
                .thenReturn(List.of(metric));
        when(findingRepository
                .findAllByRun_RunIdAndRun_Owner_UserIdOrderByRuleIdAscFingerprintAscFindingIdAsc(
                        runId, owner.getUserId()))
                .thenReturn(List.of(highFinding, unknownFinding));

        AnalysisReportResponse response = service.getReport(
                submission.getSubmissionId(), runId, owner.getUserId());

        assertEquals(AnalysisRunStatus.COMPLETED, response.run().status());
        assertEquals(new BigDecimal("91.00"), response.run().score());
        assertEquals("A", response.run().grade());
        assertNull(response.run().errorSummary());
        assertEquals(AnalysisReportResponse.DataAvailability.COMPLETE,
                response.dataAvailability());
        assertEquals(List.of("cyclomatic_complexity"),
                response.metricsByDimension().get("complexity").stream()
                        .map(AnalysisMetricResponse::metricKey).toList());
        assertEquals(2, response.findingsSummary().totalCount());
        assertEquals(1L, response.findingsSummary().countsBySeverity()
                .get(FindingSeverity.HIGH));
        assertEquals(1L, response.findingsSummary().unclassifiedSeverityCount());
        assertNull(response.findings().get(1).severity());
        assertEquals(
                List.of("run", "dataAvailability", "metricsByDimension",
                        "findingsSummary", "findings"),
                Arrays.stream(AnalysisReportResponse.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
        assertEquals(
                List.of("runId", "submissionId", "status", "score", "grade",
                        "createdAt", "startedAt", "completedAt", "errorSummary"),
                Arrays.stream(AnalysisReportRunOverview.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
    }

    @Test
    void reportHidesAnotherOwnersSubmission() {
        when(submissionRepository.existsBySubmissionIdAndOwner_UserId(
                submission.getSubmissionId(), owner.getUserId()))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getReport(
                        submission.getSubmissionId(), UUID.randomUUID(), owner.getUserId()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(runRepository, never()).findReportOverview(
                any(UUID.class), any(UUID.class), any(UUID.class));
    }

    @Test
    void reportHidesRunNotBelongingToSubmission() {
        UUID runId = UUID.randomUUID();
        when(submissionRepository.existsBySubmissionIdAndOwner_UserId(
                submission.getSubmissionId(), owner.getUserId()))
                .thenReturn(true);
        when(runRepository.findReportOverview(
                runId, submission.getSubmissionId(), owner.getUserId()))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getReport(
                        submission.getSubmissionId(), runId, owner.getUserId()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(metricRepository, never())
                .findAllByRun_RunIdAndRun_Owner_UserIdOrderByDimensionAscMetricKeyAsc(
                        any(UUID.class), any(UUID.class));
        verify(findingRepository, never())
                .findAllByRun_RunIdAndRun_Owner_UserIdOrderByRuleIdAscFingerprintAscFindingIdAsc(
                        any(UUID.class), any(UUID.class));
    }

    @Test
    void historyRejectsInvalidPageBounds() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.listRuns(
                        submission.getSubmissionId(), owner.getUserId(), -1, 20));
        assertThrows(
                ResponseStatusException.class,
                () -> service.listRuns(
                        submission.getSubmissionId(), owner.getUserId(), 0, 101));
    }

    @Test
    void activeRunUniqueIndexConflictIsRecognizedPrecisely() {
        ConstraintViolationException violation =
                new ConstraintViolationException(
                        "duplicate active run",
                        new SQLException("duplicate key"),
                        "uq_analysis_run_one_active_per_submission");
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("duplicate", violation);

        assertEquals(true, AnalysisRunService.isActiveRunConflict(exception));
        assertEquals(
                false,
                AnalysisRunService.isActiveRunConflict(
                        new DataIntegrityViolationException(
                                "unrelated constraint")));
    }

    private AnalysisRunHistoryItemResponse historyItem(
            UUID runId,
            AnalysisRunStatus status,
            BigDecimal score,
            String grade,
            Instant createdAt,
            String errorSummary) {
        return new AnalysisRunHistoryItemResponse(
                runId,
                submission.getSubmissionId(),
                status,
                score,
                grade,
                createdAt,
                null,
                null,
                errorSummary);
    }
}
