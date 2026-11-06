package com.auditor.analysis;

import com.auditor.analysis.contract.AnalysisEngineResult;
import com.auditor.analysis.contract.AnalyzerExecutionStatus;
import com.auditor.analysis.contract.AnalyzerFinding;
import com.auditor.analysis.contract.AnalyzerMetric;
import com.auditor.analysis.contract.ParseOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyzerResultPersistenceTransaction {

    private static final Logger logger =
            LoggerFactory.getLogger(AnalyzerResultPersistenceTransaction.class);
    private static final String SCORING_FAILURE_SUMMARY =
            "Analysis scoring failed.";

    private final AnalysisRunRepository runRepository;
    private final AnalysisMetricRepository metricRepository;
    private final AnalysisFindingRepository findingRepository;
    private final AnalysisScoringService scoringService;

    public AnalyzerResultPersistenceTransaction(
            AnalysisRunRepository runRepository,
            AnalysisMetricRepository metricRepository,
            AnalysisFindingRepository findingRepository,
            AnalysisScoringService scoringService) {
        this.runRepository = runRepository;
        this.metricRepository = metricRepository;
        this.findingRepository = findingRepository;
        this.scoringService = scoringService;
    }

    @Transactional
    public void persist(UUID runId, UUID ownerId, AnalysisEngineResult result) {
        AnalysisRun run = runRepository.findByRunIdAndOwner_UserId(runId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Analysis run not found."));
        if (run.getStatus() != AnalysisRunStatus.QUEUED
                && run.getStatus() != AnalysisRunStatus.PARSING) {
            throw new IllegalStateException(
                    "Analyzer results cannot be persisted for this run state.");
        }
        validateResult(result);
        Instant now = Instant.now();
        if (run.getStatus() == AnalysisRunStatus.QUEUED) {
            run.transitionTo(
                    AnalysisRunStatus.PARSING, now, result.analyzerVersion(), null);
        }

        if (result.parseOutcome() == ParseOutcome.NOT_ATTEMPTED) {
            run.transitionTo(AnalysisRunStatus.FAILED, now, result.analyzerVersion(),
                    "Analysis failed unexpectedly.");
            runRepository.save(run);
            return;
        }

        persistMetrics(run, result.metrics());
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
                    AnalysisMetadataSanitizer.finding(finding.metadata())));
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
                    "The Java source could not be parsed.");
            runRepository.save(run);
            return;
        }

        if (result.parseOutcome() != ParseOutcome.SUCCEEDED) {
            throw new IllegalArgumentException(
                    "Analysis results require a completed parse outcome.");
        }
        run.transitionTo(
                AnalysisRunStatus.ANALYZING,
                now,
                result.analyzerVersion(),
                null);
        finishRun(run, result, now);
        runRepository.save(run);
    }

    private static void validateResult(AnalysisEngineResult result) {
        if (result == null || result.parseOutcome() == null
                || result.executionStatus() == null
                || result.metrics() == null || result.findings() == null) {
            throw new IllegalArgumentException("Analyzer result is incomplete.");
        }
        boolean hasOutput = !result.metrics().isEmpty() || !result.findings().isEmpty();
        if (result.parseOutcome() == ParseOutcome.NOT_ATTEMPTED) {
            if (result.executionStatus() != AnalyzerExecutionStatus.FAILED || hasOutput) {
                throw new IllegalArgumentException("Analyzer result state is inconsistent.");
            }
        } else if (result.parseOutcome() == ParseOutcome.FAILED) {
            if (result.executionStatus() != AnalyzerExecutionStatus.PARSE_FAILED || hasOutput) {
                throw new IllegalArgumentException("Analyzer result state is inconsistent.");
            }
        } else if (result.executionStatus() == AnalyzerExecutionStatus.PARSE_FAILED) {
            throw new IllegalArgumentException("Analyzer result state is inconsistent.");
        }
        for (AnalyzerMetric metric : result.metrics()) {
            if (metric == null || metric.dimension() == null || metric.dimension().isBlank()
                    || metric.key() == null || metric.key().isBlank()
                    || metric.numericValue() == null) {
                throw new IllegalArgumentException("Analyzer metric is incomplete.");
            }
        }
        if (result.findings().stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Analyzer finding is incomplete.");
        }
    }

    private void persistMetrics(AnalysisRun run, List<AnalyzerMetric> metrics) {
        Map<MetricIdentity, List<AnalyzerMetric>> groupedMetrics =
                new LinkedHashMap<>();
        for (AnalyzerMetric metric : metrics) {
            groupedMetrics.computeIfAbsent(
                    new MetricIdentity(metric.dimension(), metric.key()),
                    ignored -> new ArrayList<>()).add(metric);
        }
        groupedMetrics.forEach((identity, group) -> {
            AnalyzerMetric representative = group.stream()
                    .max(java.util.Comparator.comparing(
                            AnalyzerMetric::numericValue))
                    .orElseThrow();
            metricRepository.save(new AnalysisMetric(
                    run,
                    identity.dimension(),
                    identity.key(),
                    representative.numericValue(),
                    AnalysisMetadataSanitizer.metric(metricMetadata(group))));
        });
    }

    private static Map<String, Object> metricMetadata(
            List<AnalyzerMetric> metrics) {
        if (metrics.size() == 1) {
            return metrics.getFirst().metadata();
        }
        List<Map<String, Object>> observations = metrics.stream()
                .map(metric -> {
                    Map<String, Object> observation =
                            new LinkedHashMap<>(metric.metadata());
                    observation.put("numericValue", metric.numericValue());
                    return Collections.unmodifiableMap(observation);
                })
                .toList();
        return Map.of(
                "aggregation", "maximum",
                "observations", observations);
    }

    private void finishRun(
            AnalysisRun run,
            AnalysisEngineResult result,
            Instant at) {
        switch (result.executionStatus()) {
            case PARTIAL -> run.transitionTo(
                    AnalysisRunStatus.PARTIAL,
                    at,
                    result.analyzerVersion(),
                    "Analysis completed with unavailable checks.");
            case FAILED -> run.transitionTo(
                    AnalysisRunStatus.FAILED,
                    at,
                    result.analyzerVersion(),
                    "Analysis failed unexpectedly.");
            case PARSE_FAILED -> throw new IllegalArgumentException(
                    "PARSE_FAILED requires a failed parse outcome.");
            case COMPLETED -> {
                run.transitionTo(
                        AnalysisRunStatus.SCORING,
                        at,
                        result.analyzerVersion(),
                        null);
                try {
                    AnalysisScoringResult scoring = scoringService.score(result);
                    if (scoring.availability()
                            == AnalysisScoringResult.Availability.AVAILABLE) {
                        run.assignScoreAndGrade(
                                scoring.score(), scoring.grade());
                    }
                    run.transitionTo(
                            AnalysisRunStatus.COMPLETED,
                            at,
                            result.analyzerVersion(),
                            null);
                } catch (RuntimeException exception) {
                    logger.error(
                            "Scoring failed for analysis run {}.",
                            run.getRunId(),
                            exception);
                    run.transitionTo(
                            AnalysisRunStatus.FAILED,
                            at,
                            result.analyzerVersion(),
                            SCORING_FAILURE_SUMMARY);
                }
            }
        }
    }

    private record MetricIdentity(String dimension, String key) {
    }
}
