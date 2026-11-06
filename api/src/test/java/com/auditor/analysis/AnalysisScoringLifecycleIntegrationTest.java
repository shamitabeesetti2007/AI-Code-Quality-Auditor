package com.auditor.analysis;

import com.auditor.analysis.dto.AnalysisRunDetailResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AnalysisScoringLifecycleIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodeSubmissionRepository submissionRepository;

    @Autowired
    private AnalysisRunService runService;

    @Autowired
    private AnalysisOrchestrationService orchestrationService;

    @Test
    void persistsAnalysisMetricsScoreAndGradeThroughRunLifecycle() {
        AppUser owner = userRepository.saveAndFlush(new AppUser(
                "scoring-" + UUID.randomUUID() + "@example.com",
                "test-password-hash",
                "Scoring integration"));
        String source = """
                class Example {
                    void simple() {}
                    void branches(boolean first, boolean second) {
                        if (first) {
                            if (second) {}
                        }
                    }
                }
                """;
        SubmissionSourceProcessor.NormalizedSource normalized =
                new SubmissionSourceProcessor().process(source);
        CodeSubmission submission = submissionRepository.saveAndFlush(
                new CodeSubmission(
                        owner,
                        "Scoring integration",
                        SubmissionLanguage.JAVA,
                        SubmissionOriginType.HUMAN,
                        normalized.sourceCode(),
                        normalized.contentHash(),
                        normalized.logicalLoc()));
        var queuedRun = runService.createQueuedRun(
                submission.getSubmissionId(), owner.getUserId());

        orchestrationService.analyze(queuedRun.runId(), owner.getUserId());

        AnalysisRunDetailResponse detail =
                runService.getRun(queuedRun.runId(), owner.getUserId());
        assertEquals(AnalysisRunStatus.COMPLETED, detail.run().status());
        assertNotNull(detail.score());
        assertNotNull(detail.grade());
        assertTrue(detail.score().signum() >= 0);
        assertTrue(detail.score().compareTo(java.math.BigDecimal.valueOf(100)) <= 0);
                assertEquals(12, detail.metrics().size());
        assertEquals(2, detail.metrics().stream()
                .filter(metric -> metric.metadata().get("observations")
                        instanceof java.util.List<?> observations
                        && observations.stream().anyMatch(observation ->
                                observation instanceof java.util.Map<?, ?> values
                                        && "java-complexity".equals(
                                                values.get("analyzerId"))))
                .count());
        assertNotNull(detail.run().completedAt());
    }
}
