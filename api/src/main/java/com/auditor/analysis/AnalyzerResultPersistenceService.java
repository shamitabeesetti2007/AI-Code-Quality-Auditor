package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.AnalyzerFinding;
import com.auditor.analysis.contract.AnalyzerMetric;
import com.auditor.analysis.contract.ParseOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class AnalyzerResultPersistenceService {

    private final AnalysisRunRepository runRepository;
    private final AnalysisMetricRepository metricRepository;
    private final AnalysisFindingRepository findingRepository;

    public AnalyzerResultPersistenceService(
            AnalysisRunRepository runRepository,
            AnalysisMetricRepository metricRepository,
            AnalysisFindingRepository findingRepository) {
        this.runRepository = runRepository;
        this.metricRepository = metricRepository;
        this.findingRepository = findingRepository;
    }

    @Transactional
    public void persist(UUID runId, UUID ownerId, AnalysisEngineResult result) {
        AnalysisRun run = runRepository.findByRunIdAndOwner_UserId(runId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Analysis run not found."));
        Instant now = Instant.now();
        if (run.getStatus() == AnalysisRunStatus.QUEUED) {
            run.transitionTo(
                    AnalysisRunStatus.PARSING, now, result.analyzerVersion(), null);
        }

        for (AnalyzerMetric metric : result.metrics()) {
            metricRepository.save(new AnalysisMetric(
                    run,
                    metric.dimension(),
                    metric.key(),
                    metric.numericValue(),
                    metric.metadata()));
        }
        for (AnalyzerFinding finding : result.findings()) {
            findingRepository.save(new AnalysisFinding(
                    run,
                    finding.ruleId(),
                    finding.fingerprint(),
                    finding.severity() == null
                            ? null
                            : FindingSeverity.valueOf(finding.severity().name()),
                    finding.message(),
                    finding.description(),
                    finding.filePath(),
                    finding.startLine(),
                    finding.startColumn(),
                    finding.endLine(),
                    finding.endColumn(),
                    finding.cweId(),
                    finding.metadata()));
        }

        if (result.parseOutcome() == ParseOutcome.FAILED) {
            if (result.executionStatus() != AnalyzerExecutionStatus.PARSE_FAILED) {
                throw new IllegalArgumentException(
                        "A failed parse must have PARSE_FAILED execution status.");
            }
            run.transitionTo(
                    AnalysisRunStatus.PARSE_FAILED,
                    now,
                    result.analyzerVersion(),
                    result.errorSummary());
        } else {
            if (result.parseOutcome() != ParseOutcome.SUCCEEDED) {
                throw new IllegalArgumentException(
                        "Analysis results require a completed parse outcome.");
            }
            if (run.getStatus() == AnalysisRunStatus.PARSING) {
                run.transitionTo(
                        AnalysisRunStatus.ANALYZING,
                        now,
                        result.analyzerVersion(),
                        null);
            }
            finishRun(run, result, now);
        }
        runRepository.save(run);
    }

    private static void finishRun(
            AnalysisRun run,
            AnalysisEngineResult result,
            Instant at) {
        AnalysisRunStatus terminalStatus =
                switch (result.executionStatus()) {
                    case COMPLETED -> AnalysisRunStatus.COMPLETED;
                    case PARTIAL -> AnalysisRunStatus.PARTIAL;
                    case PARSE_FAILED -> throw new IllegalArgumentException(
                            "PARSE_FAILED requires a failed parse outcome.");
                    case FAILED -> AnalysisRunStatus.FAILED;
                };
        if (terminalStatus == AnalysisRunStatus.COMPLETED
                && run.getStatus() == AnalysisRunStatus.ANALYZING) {
            run.transitionTo(
                    AnalysisRunStatus.SCORING,
                    at,
                    result.analyzerVersion(),
                    null);
        }
        run.transitionTo(
                terminalStatus,
                at,
                result.analyzerVersion(),
                result.errorSummary());
    }
}
