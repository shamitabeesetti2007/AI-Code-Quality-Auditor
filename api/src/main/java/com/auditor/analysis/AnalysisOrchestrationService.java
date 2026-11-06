package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.CodeAnalyzer;
import com.auditor.submission.SubmissionSourceProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@Service
public class AnalysisOrchestrationService {

    private static final String ENGINE_FAILURE_SUMMARY =
            "Analysis engine failed unexpectedly.";

    private final AnalysisRunRepository runRepository;
    private final AnalysisRunService runService;
    private final CodeAnalyzer codeAnalyzer;
    private final AnalyzerResultPersistenceService resultPersistenceService;
    private final SubmissionSourceProcessor sourceProcessor;

    public AnalysisOrchestrationService(
            AnalysisRunRepository runRepository,
            AnalysisRunService runService,
            CodeAnalyzer codeAnalyzer,
            AnalyzerResultPersistenceService resultPersistenceService,
            SubmissionSourceProcessor sourceProcessor) {
        this.runRepository = runRepository;
        this.runService = runService;
        this.codeAnalyzer = codeAnalyzer;
        this.resultPersistenceService = resultPersistenceService;
        this.sourceProcessor = sourceProcessor;
    }

    public void analyze(UUID runId, UUID ownerId) {
        AnalysisRun run = runRepository.findByRunIdAndOwner_UserId(runId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Analysis run not found."));
        if (run.getStatus() != AnalysisRunStatus.QUEUED) {
            throw new IllegalStateException(
                    "Only queued analysis runs can be analyzed.");
        }

        String normalizedSnapshot =
                sourceProcessor.process(run.getSourceSnapshot()).sourceCode();
        runService.transition(
                runId, ownerId, AnalysisRunStatus.PARSING, null, null);

        AnalysisEngineResult result;
        try {
            result = Objects.requireNonNull(
                    codeAnalyzer.analyze(normalizedSnapshot),
                    "Analysis engine returned no result.");
        } catch (RuntimeException exception) {
            runService.transition(
                    runId,
                    ownerId,
                    AnalysisRunStatus.FAILED,
                    null,
                    ENGINE_FAILURE_SUMMARY);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Analysis engine failed.");
        }

        resultPersistenceService.persist(runId, ownerId, result);
    }
}
