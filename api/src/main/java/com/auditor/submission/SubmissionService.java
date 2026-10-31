package com.auditor.submission;

import com.auditor.analysis.AnalysisRunService;
import com.auditor.analysis.dto.AnalysisRunSummaryResponse;
import com.auditor.submission.dto.CreateSubmissionRequest;
import com.auditor.submission.dto.PageResponse;
import com.auditor.submission.dto.SubmissionDetailResponse;
import com.auditor.submission.dto.SubmissionResponse;
import com.auditor.submission.dto.UpdateSubmissionRequest;
import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class SubmissionService {

    private final CodeSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final SubmissionSourceProcessor sourceProcessor;
    private final AnalysisRunService analysisRunService;

    public SubmissionService(
            CodeSubmissionRepository submissionRepository,
            UserRepository userRepository,
            SubmissionSourceProcessor sourceProcessor,
            AnalysisRunService analysisRunService) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.sourceProcessor = sourceProcessor;
        this.analysisRunService = analysisRunService;
    }

    @Transactional
    public CreatedSubmission create(UUID ownerId, CreateSubmissionRequest request) {
        AppUser owner = requireActiveOwner(ownerId);
        SubmissionLanguage language = SubmissionLanguage.valueOf(
                sourceProcessor.normalizeLanguage(request.getLanguage()));
        SubmissionOriginType origin = sourceProcessor.parseClientOrigin(
                request.getOriginType());
        SubmissionSourceProcessor.NormalizedSource source =
                sourceProcessor.process(request.getSourceCode());

        CodeSubmission submission = submissionRepository.saveAndFlush(
                new CodeSubmission(
                        owner,
                        request.getTitle().strip(),
                        language,
                        origin,
                        source.sourceCode(),
                        source.contentHash(),
                        source.logicalLoc()));

        AnalysisRunSummaryResponse autoAnalysisRun =
                request.isAutoAnalyze()
                        ? analysisRunService.createQueuedRun(
                                submission.getSubmissionId(), ownerId)
                        : null;
        return new CreatedSubmission(
                SubmissionResponse.from(submission), autoAnalysisRun);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> list(
            UUID ownerId,
            int page,
            int size) {
        validatePage(page, size);
        Page<CodeSubmission> submissions =
                submissionRepository.findAllByOwner_UserId(
                        ownerId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(submissions.map(SubmissionResponse::from));
    }

    @Transactional(readOnly = true)
    public SubmissionDetailResponse get(UUID ownerId, UUID submissionId) {
        return SubmissionDetailResponse.from(findOwned(ownerId, submissionId));
    }

    @Transactional
    public SubmissionResponse update(
            UUID ownerId,
            UUID submissionId,
            UpdateSubmissionRequest request) {
        CodeSubmission submission = findOwned(ownerId, submissionId);
        SubmissionSourceProcessor.NormalizedSource source =
                sourceProcessor.process(request.getSourceCode());
        submission.updateContent(
                request.getTitle().strip(),
                source.sourceCode(),
                source.contentHash(),
                source.logicalLoc());
        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    @Transactional
    public void delete(UUID ownerId, UUID submissionId) {
        submissionRepository.delete(findOwned(ownerId, submissionId));
    }

    private CodeSubmission findOwned(UUID ownerId, UUID submissionId) {
        return submissionRepository
                .findBySubmissionIdAndOwner_UserId(submissionId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Submission not found."));
    }

    private AppUser requireActiveOwner(UUID ownerId) {
        return userRepository.findById(ownerId)
                .filter(user -> user.getAccountStatus()
                        == AppUser.AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authentication required."));
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100.");
        }
    }

    public record CreatedSubmission(
            SubmissionResponse submission,
            AnalysisRunSummaryResponse autoAnalysisRun) {
    }
}
