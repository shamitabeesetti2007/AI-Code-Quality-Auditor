package com.auditor.analysis;

import com.auditor.submission.CodeSubmission;
import com.auditor.submission.CodeSubmissionRepository;
import com.auditor.submission.SubmissionLanguage;
import com.auditor.submission.SubmissionOriginType;
import com.auditor.user.AppUser;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
}
