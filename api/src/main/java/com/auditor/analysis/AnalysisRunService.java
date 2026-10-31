package com.auditor.analysis;

import com.auditor.analysis.dto.AnalysisFindingResponse;
import com.auditor.analysis.dto.AnalysisMetricResponse;
import com.auditor.analysis.dto.AnalysisRunDetailResponse;
import com.auditor.analysis.dto.AnalysisRunSummaryResponse;
import com.auditor.submission.CodeSubmission;
import com.auditor.submission.CodeSubmissionRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisRunService {

    private static final String ACTIVE_RUN_INDEX =
            "uq_analysis_run_one_active_per_submission";

    private final AnalysisRunRepository runRepository;
    private final AnalysisMetricRepository metricRepository;
    private final AnalysisFindingRepository findingRepository;
    private final CodeSubmissionRepository submissionRepository;

    public AnalysisRunService(
            AnalysisRunRepository runRepository,
            AnalysisMetricRepository metricRepository,
            AnalysisFindingRepository findingRepository,
            CodeSubmissionRepository submissionRepository) {
        this.runRepository = runRepository;
        this.metricRepository = metricRepository;
        this.findingRepository = findingRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public AnalysisRunSummaryResponse createQueuedRun(
            UUID submissionId,
            UUID ownerId) {
        CodeSubmission submission = findOwnedSubmission(ownerId, submissionId);
        AnalysisRun run = new AnalysisRun(
                submission,
                submission.getOwner(),
                submission.getSourceCode(),
                submission.getContentHash());
        return AnalysisRunSummaryResponse.from(runRepository.saveAndFlush(run));
    }

    @Transactional(readOnly = true)
    public List<AnalysisRunSummaryResponse> listRuns(
            UUID submissionId,
            UUID ownerId) {
        findOwnedSubmission(ownerId, submissionId);
        return runRepository
                .findAllBySubmission_SubmissionIdAndOwner_UserIdOrderByCreatedAtDesc(
                        submissionId, ownerId)
                .stream()
                .map(AnalysisRunSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisRunDetailResponse getRun(UUID runId, UUID ownerId) {
        AnalysisRun run = findOwnedRun(ownerId, runId);
        List<AnalysisMetricResponse> metrics = metricRepository
                .findAllByRun_RunIdAndRun_Owner_UserId(runId, ownerId)
                .stream()
                .map(AnalysisMetricResponse::from)
                .toList();
        List<AnalysisFindingResponse> findings = findingRepository
                .findAllByRun_RunIdAndRun_Owner_UserId(runId, ownerId)
                .stream()
                .map(AnalysisFindingResponse::from)
                .toList();
        return AnalysisRunDetailResponse.from(run, metrics, findings);
    }

    @Transactional
    public AnalysisRun transition(
            UUID runId,
            UUID ownerId,
            AnalysisRunStatus nextStatus,
            String analyzerVersion,
            String errorSummary) {
        AnalysisRun run = findOwnedRun(ownerId, runId);
        run.transitionTo(nextStatus, Instant.now(), analyzerVersion, errorSummary);
        return runRepository.save(run);
    }

    private CodeSubmission findOwnedSubmission(
            UUID ownerId,
            UUID submissionId) {
        return submissionRepository
                .findBySubmissionIdAndOwner_UserId(submissionId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Submission not found."));
    }

    private AnalysisRun findOwnedRun(UUID ownerId, UUID runId) {
        return runRepository.findByRunIdAndOwner_UserId(runId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Analysis run not found."));
    }

    public static boolean isActiveRunConflict(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && ACTIVE_RUN_INDEX.equals(violation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
