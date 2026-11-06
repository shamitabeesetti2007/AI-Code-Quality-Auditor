package com.auditor.analysis;

import com.auditor.analysis.dto.AnalysisRunComparisonResponse;
import com.auditor.submission.*;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AnalysisRunComparisonIntegrationTest {
    @Autowired UserRepository users;
    @Autowired CodeSubmissionRepository submissions;
    @Autowired AnalysisRunRepository runs;
    @Autowired AnalysisMetricRepository metrics;
    @Autowired AnalysisFindingRepository findings;
    @Autowired AnalysisRunService service;

    @Test
    void comparesScoresMetricsAndFingerprintMatchedFindingsWithoutSource() {
        AppUser owner = owner(); CodeSubmission submission = submission(owner);
        AnalysisRun baseline = completed(submission, owner, "A");
        AnalysisRun target = completed(submission, owner, "B");
        metrics.save(new AnalysisMetric(baseline, "complexity", "cyclomatic", new BigDecimal("4"), Map.of()));
        metrics.save(new AnalysisMetric(target, "complexity", "cyclomatic", new BigDecimal("7"), Map.of()));
        metrics.save(new AnalysisMetric(target, "quality", "new_metric", BigDecimal.ONE, Map.of()));
        finding(baseline, "1".repeat(64), "retained");
        finding(baseline, "2".repeat(64), "resolved");
        finding(target, "1".repeat(64), "retained");
        finding(target, "3".repeat(64), "added");

        AnalysisRunComparisonResponse result = service.compareRuns(submission.getSubmissionId(),
                baseline.getRunId(), target.getRunId(), owner.getUserId());
        assertEquals(0, new BigDecimal("15").compareTo(result.scoreDifference()));
        assertEquals(AnalysisRunComparisonResponse.Availability.COMPLETE, result.comparisonAvailability());
        assertEquals(2, result.metrics().items().size());
        assertEquals(new BigDecimal("3"), result.metrics().items().getFirst().difference());
        assertEquals(1L, result.findings().counts().get(AnalysisRunComparisonResponse.FindingChange.ADDED));
        assertEquals(1L, result.findings().counts().get(AnalysisRunComparisonResponse.FindingChange.RESOLVED));
        assertEquals(1L, result.findings().counts().get(AnalysisRunComparisonResponse.FindingChange.PERSISTING));
        assertFalse(result.toString().contains("COMPARISON_SOURCE_SECRET"));
    }

    @Test
    void unavailableAndPartialRunsAreNotReportedAsCompleteAndIdsAreValidated() {
        AppUser owner = owner(); CodeSubmission submission = submission(owner);
        AnalysisRun failed = runs.saveAndFlush(new AnalysisRun(submission, owner,
                "class COMPARISON_SOURCE_SECRET {}", "0".repeat(64)));
        failed.transitionTo(AnalysisRunStatus.FAILED, Instant.now(), null, "secret exception");
        runs.saveAndFlush(failed);
        AnalysisRun partial = runs.saveAndFlush(new AnalysisRun(submission, owner,
                "class COMPARISON_SOURCE_SECRET {}", "1".repeat(64)));
        partial.transitionTo(AnalysisRunStatus.PARSING, Instant.now(), null, null);
        partial.transitionTo(AnalysisRunStatus.ANALYZING, Instant.now(), null, null);
        partial.transitionTo(AnalysisRunStatus.PARTIAL, Instant.now(), null, "internal details");
        runs.saveAndFlush(partial);
        AnalysisRunComparisonResponse result = service.compareRuns(submission.getSubmissionId(),
                failed.getRunId(), partial.getRunId(), owner.getUserId());
        assertNull(result.scoreDifference());
        assertEquals(AnalysisRunComparisonResponse.Availability.PARTIAL, result.comparisonAvailability());
        assertEquals(AnalysisRunComparisonResponse.DimensionAvailability.PARTIAL,
                result.metrics().dataAvailability());
        assertThrows(ResponseStatusException.class, () -> service.compareRuns(submission.getSubmissionId(),
                failed.getRunId(), failed.getRunId(), owner.getUserId()));
        assertThrows(ResponseStatusException.class, () -> service.compareRuns(submission.getSubmissionId(),
                failed.getRunId(), UUID.randomUUID(), owner.getUserId()));
    }

    @Test
    void duplicateFingerprintsAreUnmatchedAndNullableSeverityIsSafe() {
        AppUser owner = owner(); CodeSubmission submission = submission(owner);
        AnalysisRun baseline = completed(submission, owner, "A");
        AnalysisRun target = completed(submission, owner, "B");
        String duplicate = "4".repeat(64);
        findings.save(new AnalysisFinding(baseline, "RULE_TEST", duplicate, null,
                "unclassified duplicate", null, null, null, null, null, null, null, Map.of()));
        finding(baseline, duplicate, "classified duplicate");
        finding(target, duplicate, "target duplicate");
        AnalysisRunComparisonResponse result = service.compareRuns(submission.getSubmissionId(),
                baseline.getRunId(), target.getRunId(), owner.getUserId());
        assertEquals(2L, result.findings().counts().get(AnalysisRunComparisonResponse.FindingChange.RESOLVED));
        assertEquals(1L, result.findings().counts().get(AnalysisRunComparisonResponse.FindingChange.ADDED));
        assertEquals(0L, result.findings().counts().get(AnalysisRunComparisonResponse.FindingChange.PERSISTING));
        assertTrue(result.findings().items().stream().anyMatch(item -> item.severity() == null));
        assertEquals(1L, result.findings().unclassifiedSeverityCounts()
                .get(AnalysisRunComparisonResponse.FindingChange.RESOLVED));
    }

    @Test
    void partialAndUnavailableDimensionsAreNeverMarkedComplete() {
        AppUser owner = owner(); CodeSubmission submission = submission(owner);
        AnalysisRun completed = completed(submission, owner, "A");
        AnalysisRun partialOne = partial(submission, owner);
        AnalysisRun partialTwo = partial(submission, owner);
        AnalysisRun failedOne = failed(submission, owner);
        AnalysisRun failedTwo = failed(submission, owner);
        metrics.save(new AnalysisMetric(completed, "quality", "size", BigDecimal.ONE, Map.of()));
        metrics.save(new AnalysisMetric(partialOne, "quality", "size", BigDecimal.valueOf(2), Map.of()));
        finding(partialOne, "5".repeat(64), "partial finding");
        finding(partialTwo, "6".repeat(64), "other partial finding");

        var completedPartial = service.compareRuns(submission.getSubmissionId(), completed.getRunId(),
                partialOne.getRunId(), owner.getUserId());
        assertEquals(AnalysisRunComparisonResponse.DimensionAvailability.PARTIAL,
                completedPartial.metrics().dataAvailability());
        assertEquals(AnalysisRunComparisonResponse.DimensionAvailability.PARTIAL,
                completedPartial.findings().dataAvailability());

        var partialPartial = service.compareRuns(submission.getSubmissionId(), partialOne.getRunId(),
                partialTwo.getRunId(), owner.getUserId());
        assertEquals(AnalysisRunComparisonResponse.DimensionAvailability.PARTIAL,
                partialPartial.metrics().dataAvailability());
        assertEquals(AnalysisRunComparisonResponse.DimensionAvailability.PARTIAL,
                partialPartial.findings().dataAvailability());

        var completedFailed = service.compareRuns(submission.getSubmissionId(), completed.getRunId(),
                failedOne.getRunId(), owner.getUserId());
        assertEquals(AnalysisRunComparisonResponse.DimensionAvailability.PARTIAL,
                completedFailed.metrics().dataAvailability());
        assertTrue(completedFailed.metrics().items().isEmpty());
        assertTrue(completedFailed.findings().items().isEmpty());

        var failedFailed = service.compareRuns(submission.getSubmissionId(), failedOne.getRunId(),
                failedTwo.getRunId(), owner.getUserId());
        assertEquals(AnalysisRunComparisonResponse.DimensionAvailability.UNAVAILABLE,
                failedFailed.metrics().dataAvailability());
        assertEquals(AnalysisRunComparisonResponse.DimensionAvailability.UNAVAILABLE,
                failedFailed.findings().dataAvailability());
    }

    private AppUser owner() {
        return users.saveAndFlush(new AppUser("compare-" + UUID.randomUUID() + "@example.com",
                "test-hash", "Compare test"));
    }
    private CodeSubmission submission(AppUser owner) {
        var source = new SubmissionSourceProcessor().process("class COMPARISON_SOURCE_SECRET {}");
        return submissions.saveAndFlush(new CodeSubmission(owner, "Compare", SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN, source.sourceCode(), source.contentHash(), source.logicalLoc()));
    }
    private AnalysisRun completed(CodeSubmission submission, AppUser owner, String score) {
        AnalysisRun run = new AnalysisRun(submission, owner, "class COMPARISON_SOURCE_SECRET {}", "a".repeat(64));
        run.transitionTo(AnalysisRunStatus.PARSING, Instant.now(), null, null);
        run.transitionTo(AnalysisRunStatus.ANALYZING, Instant.now(), null, null);
        run.transitionTo(AnalysisRunStatus.SCORING, Instant.now(), null, null);
        run.assignScoreAndGrade(new BigDecimal(score.equals("A") ? "70" : "85"), score);
        run.transitionTo(AnalysisRunStatus.COMPLETED, Instant.now(), null, null);
        return runs.saveAndFlush(run);
    }
    private void finding(AnalysisRun run, String fingerprint, String message) {
        findings.save(new AnalysisFinding(run, "RULE_TEST", fingerprint, FindingSeverity.MEDIUM,
                message, null, "Example.java", 2, 1, 2, 4, null, Map.of()));
    }
    private AnalysisRun partial(CodeSubmission submission, AppUser owner) {
        AnalysisRun run = createRun(submission, owner);
        run.transitionTo(AnalysisRunStatus.PARSING, Instant.now(), null, null);
        run.transitionTo(AnalysisRunStatus.ANALYZING, Instant.now(), null, null);
        run.transitionTo(AnalysisRunStatus.PARTIAL, Instant.now(), null, "internal detail");
        return runs.saveAndFlush(run);
    }
    private AnalysisRun failed(CodeSubmission submission, AppUser owner) {
        AnalysisRun run = createRun(submission, owner);
        run.transitionTo(AnalysisRunStatus.FAILED, Instant.now(), null, "internal detail");
        return runs.saveAndFlush(run);
    }
    private AnalysisRun createRun(CodeSubmission submission, AppUser owner) {
        return new AnalysisRun(submission, owner, "class COMPARISON_SOURCE_SECRET {}",
                "2".repeat(64));
    }
}
