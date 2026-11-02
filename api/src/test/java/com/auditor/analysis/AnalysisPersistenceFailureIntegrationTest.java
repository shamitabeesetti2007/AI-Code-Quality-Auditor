package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.AnalyzerFinding;
import com.auditor.analysis.contract.AnalyzerMetric;
import com.auditor.analysis.contract.ParseOutcome;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AnalysisPersistenceFailureIntegrationTest {
    @Autowired UserRepository users;
    @Autowired CodeSubmissionRepository submissions;
    @Autowired AnalysisRunRepository runs;
    @Autowired AnalysisMetricRepository metrics;
    @Autowired AnalysisFindingRepository findings;
    @Autowired AnalysisRunService runService;
    @Autowired AnalyzerResultPersistenceService persistence;

    @Test
    void invalidResultRollsBackAndPersistsSafeFailedStateSeparately() {
        AppUser owner = users.saveAndFlush(new AppUser(
                "persist-failure-" + UUID.randomUUID() + "@example.com",
                "test-hash", "Persistence failure"));
        var normalized = new SubmissionSourceProcessor().process("class Example {}");
        CodeSubmission submission = submissions.saveAndFlush(new CodeSubmission(owner,
                "Persistence failure", SubmissionLanguage.JAVA, SubmissionOriginType.HUMAN,
                normalized.sourceCode(), normalized.contentHash(), normalized.logicalLoc()));
        var queued = runService.createQueuedRun(submission.getSubmissionId(), owner.getUserId());
        AnalysisRun run = runs.findByRunIdAndOwner_UserId(queued.runId(), owner.getUserId()).orElseThrow();
        run.transitionTo(AnalysisRunStatus.PARSING, java.time.Instant.now(), null, null);
        runs.saveAndFlush(run);

        AnalysisEngineResult invalid = new AnalysisEngineResult(ParseOutcome.SUCCEEDED, "engine",
                AnalyzerExecutionStatus.COMPLETED, "must not leak",
                List.of(new AnalyzerFinding("RULE_INVALID", "invalid-fingerprint", null,
                        "Invalid finding", null, null, null, null, null, null, null, Map.of())),
                List.of(new AnalyzerMetric("quality", "will-rollback", java.math.BigDecimal.ONE,
                        Map.of())));
        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> persistence.persist(run.getRunId(), owner.getUserId(), invalid));

        AnalysisRun persisted = runs.findByRunIdAndOwner_UserId(run.getRunId(), owner.getUserId())
                .orElseThrow();
        assertEquals(500, failure.getStatusCode().value());
        assertEquals(AnalysisRunStatus.FAILED, persisted.getStatus());
        assertEquals("Analysis failed unexpectedly.", persisted.getErrorSummary());
        assertTrue(metrics.findAllByRun_RunIdAndRun_Owner_UserId(run.getRunId(), owner.getUserId())
                .isEmpty());
        assertTrue(findings.findAllByRun_RunIdAndRun_Owner_UserId(run.getRunId(), owner.getUserId())
                .isEmpty());
    }
}
