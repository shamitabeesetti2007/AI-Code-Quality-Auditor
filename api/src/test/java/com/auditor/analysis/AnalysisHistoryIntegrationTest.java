package com.auditor.analysis;

import com.auditor.analysis.dto.AnalysisRunHistoryResponse;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AnalysisHistoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodeSubmissionRepository submissionRepository;

    @Autowired
    private AnalysisRunRepository runRepository;

    @Autowired
    private AnalysisRunService runService;

    @Test
    void retrievesStableNewestFirstHistoryAcrossDatabasePages() {
        AppUser owner = createOwner();
        CodeSubmission submission = createSubmission(owner);
        Instant sameCreatedAt = Instant.parse("2026-10-30T10:00:00Z");
        List<AnalysisRun> runs = List.of(
                createRun(submission, owner, 1, sameCreatedAt, AnalysisRunStatus.QUEUED),
                createRun(submission, owner, 2, sameCreatedAt, AnalysisRunStatus.PARTIAL),
                createRun(submission, owner, 3, sameCreatedAt, AnalysisRunStatus.COMPLETED));
        runs.forEach(runRepository::saveAndFlush);

        AnalysisRunHistoryResponse firstPage = runService.listRuns(
                submission.getSubmissionId(), owner.getUserId(), 0, 2);
        AnalysisRunHistoryResponse secondPage = runService.listRuns(
                submission.getSubmissionId(), owner.getUserId(), 1, 2);

        List<UUID> expectedOrder = runs.stream()
                .map(AnalysisRun::getRunId)
                .sorted(Comparator.comparing(UUID::toString).reversed())
                .toList();
        assertEquals(
                expectedOrder.subList(0, 2),
                firstPage.items().stream()
                        .map(item -> item.runId())
                        .toList());
        assertEquals(
                expectedOrder.subList(2, 3),
                secondPage.items().stream()
                        .map(item -> item.runId())
                        .toList());
        assertEquals(
                "Analysis completed with unavailable checks.",
                firstPage.items().stream()
                        .filter(item -> item.status() == AnalysisRunStatus.PARTIAL)
                        .findFirst()
                        .orElseThrow()
                        .errorSummary());
        assertEquals(3, firstPage.totalItems());
        assertEquals(2, firstPage.totalPages());
        assertEquals(1, secondPage.page());

        var completed = firstPage.items().stream()
                .filter(item -> item.status() == AnalysisRunStatus.COMPLETED)
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("93.00"), completed.score());
        assertEquals("A", completed.grade());
        assertEquals(sameCreatedAt, completed.createdAt());
        assertTrue(completed.startedAt().isBefore(completed.completedAt()));

        assertEquals(
                List.of("runId", "submissionId", "status", "score", "grade",
                        "createdAt", "startedAt", "completedAt", "errorSummary"),
                java.util.Arrays.stream(firstPage.items().getFirst().getClass()
                                .getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
    }

    @Test
    void returnsEmptyHistoryAndDoesNotExposeAnotherOwnersSubmission() {
        AppUser owner = createOwner();
        AppUser otherOwner = createOwner();
        CodeSubmission ownedSubmission = createSubmission(owner);
        CodeSubmission otherSubmission = createSubmission(otherOwner);

        AnalysisRunHistoryResponse empty = runService.listRuns(
                ownedSubmission.getSubmissionId(), owner.getUserId(), 0, 20);
        assertTrue(empty.items().isEmpty());
        assertEquals(0, empty.totalItems());
        assertEquals(0, empty.totalPages());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> runService.listRuns(
                        otherSubmission.getSubmissionId(),
                        owner.getUserId(),
                        0,
                        20));
        assertEquals(404, exception.getStatusCode().value());
    }

    private AppUser createOwner() {
        return userRepository.saveAndFlush(new AppUser(
                "history-" + UUID.randomUUID() + "@example.com",
                "history-test-hash",
                "History test"));
    }

    private CodeSubmission createSubmission(AppUser owner) {
        String source = "class CONFIDENTIAL_SOURCE {}";
        SubmissionSourceProcessor.NormalizedSource normalized =
                new SubmissionSourceProcessor().process(source);
        return submissionRepository.saveAndFlush(new CodeSubmission(
                owner,
                "History test",
                SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN,
                normalized.sourceCode(),
                normalized.contentHash(),
                normalized.logicalLoc()));
    }

    private AnalysisRun createRun(
            CodeSubmission submission,
            AppUser owner,
            long idSuffix,
            Instant createdAt,
            AnalysisRunStatus terminalStatus) {
        AnalysisRun run = new AnalysisRun(
                submission,
                owner,
                "class CONFIDENTIAL_SOURCE {}",
                "0".repeat(64));
        ReflectionTestUtils.setField(
                run,
                "runId",
                UUID.fromString("00000000-0000-0000-0000-"
                        + String.format("%012d", idSuffix)));
        ReflectionTestUtils.setField(run, "createdAt", createdAt);
        if (terminalStatus != AnalysisRunStatus.QUEUED) {
            Instant startedAt = createdAt.plusSeconds(10);
            run.transitionTo(AnalysisRunStatus.PARSING, startedAt, null, null);
            run.transitionTo(AnalysisRunStatus.ANALYZING, startedAt, null, null);
            if (terminalStatus == AnalysisRunStatus.PARTIAL) {
                run.transitionTo(
                        AnalysisRunStatus.PARTIAL,
                        startedAt.plusSeconds(5),
                        null,
                        "private engine exception information");
            } else {
                run.transitionTo(AnalysisRunStatus.SCORING, startedAt, null, null);
                run.assignScoreAndGrade(new BigDecimal("93.00"), "A");
                run.transitionTo(
                        AnalysisRunStatus.COMPLETED,
                        startedAt.plusSeconds(5),
                        null,
                        null);
            }
        }
        return run;
    }
}
