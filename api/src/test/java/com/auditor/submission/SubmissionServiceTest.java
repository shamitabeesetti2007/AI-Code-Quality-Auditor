package com.auditor.submission;

import com.auditor.analysis.AnalysisRunService;
import com.auditor.analysis.AnalysisFindingRepository;
import com.auditor.analysis.AnalysisMetricRepository;
import com.auditor.analysis.AnalysisRun;
import com.auditor.analysis.AnalysisRunRepository;
import com.auditor.analysis.dto.AnalysisRunSummaryResponse;
import com.auditor.submission.dto.CreateSubmissionRequest;
import com.auditor.submission.dto.UpdateSubmissionRequest;
import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class SubmissionServiceTest {

    private final CodeSubmissionRepository submissionRepository =
            mock(CodeSubmissionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AnalysisRunService analysisRunService =
            mock(AnalysisRunService.class);
    private final SubmissionSourceProcessor sourceProcessor =
            new SubmissionSourceProcessor();
    private final SubmissionService service = new SubmissionService(
            submissionRepository,
            userRepository,
            sourceProcessor,
            analysisRunService);

    private UUID ownerId;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        owner = new AppUser("owner@example.com", "password-hash", "Owner");
        ownerId = owner.getUserId();
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    }

    @Test
    void createUsesAuthenticatedOwnerAndDoesNotVerifyDeclaredProvenance() {
        when(submissionRepository.saveAndFlush(any(CodeSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AnalysisRunSummaryResponse autoRun = mock(
                AnalysisRunSummaryResponse.class);
        when(analysisRunService.createQueuedRun(any(UUID.class), eq(ownerId)))
                .thenReturn(autoRun);

        CreateSubmissionRequest request = request();
        request.setOriginType("AI_GENERATED");
        SubmissionService.CreatedSubmission created =
                service.create(ownerId, request);

        assertEquals(ownerId, getOwnerIdFromSavedSubmission());
        assertFalse(created.submission().provenanceVerified());
        assertEquals(autoRun, created.autoAnalysisRun());
    }

    @Test
    void createCanSkipAutomaticAnalysis() {
        when(submissionRepository.saveAndFlush(any(CodeSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CreateSubmissionRequest request = request();
        request.setAutoAnalyze(false);

        SubmissionService.CreatedSubmission created =
                service.create(ownerId, request);

        assertNull(created.autoAnalysisRun());
        verify(analysisRunService, never())
                .createQueuedRun(any(UUID.class), any(UUID.class));
    }

    @Test
    void automaticRunCapturesTheNewSubmissionSourceAndHash() {
        AtomicReference<CodeSubmission> savedSubmission = new AtomicReference<>();
        when(submissionRepository.saveAndFlush(any(CodeSubmission.class)))
                .thenAnswer(invocation -> {
                    CodeSubmission saved = invocation.getArgument(0);
                    savedSubmission.set(saved);
                    return saved;
                });
        when(submissionRepository.findBySubmissionIdAndOwner_UserId(
                any(UUID.class), eq(ownerId)))
                .thenAnswer(invocation -> Optional.of(savedSubmission.get()));

        AnalysisRunRepository runRepository = mock(AnalysisRunRepository.class);
        when(runRepository.saveAndFlush(any(AnalysisRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AnalysisRunService actualAnalysisRunService = new AnalysisRunService(
                runRepository,
                mock(AnalysisMetricRepository.class),
                mock(AnalysisFindingRepository.class),
                submissionRepository);
        SubmissionService serviceWithRunPersistence = new SubmissionService(
                submissionRepository,
                userRepository,
                sourceProcessor,
                actualAnalysisRunService);
        CreateSubmissionRequest request = request();
        request.setSourceCode("class AutoRun {}");

        var created = serviceWithRunPersistence.create(ownerId, request);

        ArgumentCaptor<AnalysisRun> captor =
                ArgumentCaptor.forClass(AnalysisRun.class);
        verify(runRepository).saveAndFlush(captor.capture());
        AnalysisRun run = captor.getValue();
        var processedSource = sourceProcessor.process(request.getSourceCode());
        assertEquals(processedSource.sourceCode(), run.getSourceSnapshot());
        assertEquals(processedSource.contentHash(), run.getSourceHash());
        assertEquals(run.getSourceHash(), created.autoAnalysisRun().sourceHash());
    }

    @Test
    void listIsOwnerScopedAndUsesRequestedPagination() {
        CodeSubmission submission = sampleSubmission();
        when(submissionRepository.findAllByOwner_UserId(
                eq(ownerId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(submission)));

        var page = service.list(ownerId, 0, 10);

        assertEquals(1, page.items().size());
        assertEquals(1, page.totalItems());
        verify(submissionRepository).findAllByOwner_UserId(
                eq(ownerId), any(PageRequest.class));
    }

    @Test
    void getUpdateAndDeleteAllScopeTheResourceByOwner() {
        CodeSubmission submission = sampleSubmission();
        UUID submissionId = submission.getSubmissionId();
        when(submissionRepository.findBySubmissionIdAndOwner_UserId(
                submissionId, ownerId)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(CodeSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var detail = service.get(ownerId, submissionId);
        UpdateSubmissionRequest update = new UpdateSubmissionRequest();
        update.setTitle("Changed");
        update.setSourceCode("class Changed {}");
        var updated = service.update(ownerId, submissionId, update);
        service.delete(ownerId, submissionId);

        assertEquals("class Sample {}", detail.sourceCode());
        assertEquals("Changed", updated.title());
        verify(submissionRepository, times(3))
                .findBySubmissionIdAndOwner_UserId(submissionId, ownerId);
        verify(submissionRepository).delete(submission);
    }

    @Test
    void anotherUsersSubmissionIsReportedAsNotFound() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findBySubmissionIdAndOwner_UserId(
                submissionId, ownerId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.get(ownerId, submissionId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(submissionRepository).findBySubmissionIdAndOwner_UserId(
                submissionId, ownerId);
    }

    @Test
    void requestCannotSetProvenanceVerified() {
        CreateSubmissionRequest request = request();
        when(submissionRepository.saveAndFlush(any(CodeSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        request.setAutoAnalyze(false);

        service.create(ownerId, request);

        verify(submissionRepository).saveAndFlush(
                org.mockito.ArgumentMatchers.argThat(
                        submission -> !submission.isProvenanceVerified()));
    }

    private UUID getOwnerIdFromSavedSubmission() {
        var captor = org.mockito.ArgumentCaptor.forClass(CodeSubmission.class);
        verify(submissionRepository).saveAndFlush(captor.capture());
        return captor.getValue().getOwnerUserId();
    }

    private CodeSubmission sampleSubmission() {
        return new CodeSubmission(
                owner,
                "Sample",
                SubmissionLanguage.JAVA,
                SubmissionOriginType.HUMAN,
                "class Sample {}",
                "0".repeat(64),
                1);
    }

    private static CreateSubmissionRequest request() {
        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setTitle("Sample");
        request.setLanguage("JAVA");
        request.setOriginType("HUMAN");
        request.setSourceCode("class Sample {}");
        return request;
    }
}
