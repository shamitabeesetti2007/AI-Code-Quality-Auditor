package com.auditor.analysis;

import com.auditor.analysis.dto.AnalysisFindingResponse;
import com.auditor.analysis.dto.AnalysisMetricResponse;
import com.auditor.analysis.dto.AnalysisReportResponse;
import com.auditor.analysis.dto.AnalysisReportResponse.FindingsSummary;
import com.auditor.analysis.dto.AnalysisReportRunOverview;
import com.auditor.analysis.dto.AnalysisRunDetailResponse;
import com.auditor.analysis.dto.AnalysisRunHistoryItemResponse;
import com.auditor.analysis.dto.AnalysisRunHistoryResponse;
import com.auditor.analysis.dto.AnalysisRunSummaryResponse;
import com.auditor.analysis.dto.AnalysisRunComparisonResponse;
import com.auditor.analysis.dto.AnalysisRunComparisonResponse.*;
import com.auditor.submission.CodeSubmission;
import com.auditor.submission.CodeSubmissionRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
    public AnalysisRunHistoryResponse listRuns(
            UUID submissionId,
            UUID ownerId,
            int page,
            int size) {
        validatePage(page, size);
        findOwnedSubmission(ownerId, submissionId);
        Page<AnalysisRunHistoryItemResponse> history =
                runRepository.findHistoryBySubmissionAndOwner(
                        submissionId, ownerId, PageRequest.of(page, size));
        return AnalysisRunHistoryResponse.from(
                history.map(AnalysisRunHistoryItemResponse::withSafeErrorSummary));
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

    @Transactional(readOnly = true)
    public AnalysisReportResponse getReport(
            UUID submissionId,
            UUID runId,
            UUID ownerId) {
        if (!submissionRepository.existsBySubmissionIdAndOwner_UserId(
                submissionId, ownerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Submission not found.");
        }
        AnalysisReportRunOverview overview = runRepository.findReportOverview(
                        runId, submissionId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Analysis run not found."));
        List<AnalysisMetricResponse> metrics = metricRepository
                .findAllByRun_RunIdAndRun_Owner_UserIdOrderByDimensionAscMetricKeyAsc(
                        runId, ownerId)
                .stream()
                .map(AnalysisMetricResponse::from)
                .toList();
        List<AnalysisFindingResponse> findings = findingRepository
                .findAllByRun_RunIdAndRun_Owner_UserIdOrderByRuleIdAscFingerprintAscFindingIdAsc(
                        runId, ownerId)
                .stream()
                .map(AnalysisFindingResponse::from)
                .toList();
        Map<String, List<AnalysisMetricResponse>> metricsByDimension =
                new LinkedHashMap<>();
        metrics.forEach(metric -> metricsByDimension
                .computeIfAbsent(metric.dimension(), ignored -> new ArrayList<>())
                .add(metric));

        EnumMap<FindingSeverity, Long> countsBySeverity =
                new EnumMap<>(FindingSeverity.class);
        long unclassifiedCount = 0;
        for (AnalysisFindingResponse finding : findings) {
            if (finding.severity() == null) {
                unclassifiedCount++;
            } else {
                countsBySeverity.merge(finding.severity(), 1L, Long::sum);
            }
        }
        AnalysisReportResponse.DataAvailability dataAvailability =
                AnalysisReportResponse.availabilityFor(overview.status());
        FindingsSummary findingsSummary =
                dataAvailability == AnalysisReportResponse.DataAvailability.UNAVAILABLE
                        ? null
                        : new FindingsSummary(
                                findings.size(), countsBySeverity, unclassifiedCount);
        return new AnalysisReportResponse(
                overview.withSafeErrorSummary(),
                dataAvailability,
                metricsByDimension,
                findingsSummary,
                findings);
    }

    @Transactional(readOnly = true)
    public AnalysisRunComparisonResponse compareRuns(
            UUID submissionId, UUID baselineRunId, UUID targetRunId, UUID ownerId) {
        if (baselineRunId.equals(targetRunId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Baseline and target run IDs must be different.");
        }
        if (!submissionRepository.existsBySubmissionIdAndOwner_UserId(submissionId, ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found.");
        }
        List<AnalysisRunComparisonResponse.RunOverview> runs = runRepository
                .findComparisonRuns(List.of(baselineRunId, targetRunId), submissionId, ownerId);
        Map<UUID, AnalysisRunComparisonResponse.RunOverview> byId = new HashMap<>();
        runs.forEach(run -> byId.put(run.runId(), run));
        var baseline = byId.get(baselineRunId);
        var target = byId.get(targetRunId);
        if (baseline == null || target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis run not found.");
        }
        List<UUID> ids = List.of(baselineRunId, targetRunId);
        List<AnalysisMetric> metrics = metricRepository
                .findAllByRun_RunIdInAndRun_Owner_UserIdOrderByRun_RunIdAscDimensionAscMetricKeyAsc(ids, ownerId);
        List<AnalysisFinding> findings = findingRepository
                .findAllByRun_RunIdInAndRun_Owner_UserIdOrderByRun_RunIdAscRuleIdAscFingerprintAscFindingIdAsc(ids, ownerId);
        MetricsComparison metricsComparison = compareMetrics(metrics, baselineRunId, targetRunId,
                baseline.dataAvailability(), target.dataAvailability());
        FindingsComparison findingsComparison = compareFindings(findings, baselineRunId, targetRunId,
                baseline.dataAvailability(), target.dataAvailability());
        boolean complete = baseline.status() == AnalysisRunStatus.COMPLETED
                && target.status() == AnalysisRunStatus.COMPLETED
                && metricsComparison.dataAvailability() == DimensionAvailability.COMPLETE
                && findingsComparison.dataAvailability() == DimensionAvailability.COMPLETE;
        return new AnalysisRunComparisonResponse(submissionId, baseline, target,
                baseline.score() == null || target.score() == null ? null
                        : target.score().subtract(baseline.score()),
                complete ? Availability.COMPLETE : Availability.PARTIAL,
                metricsComparison, findingsComparison);
    }

    private static MetricsComparison compareMetrics(List<AnalysisMetric> all, UUID baseline,
            UUID target, DimensionAvailability baselineAvailability,
            DimensionAvailability targetAvailability) {
        all = all.stream().filter(metric -> metric.getRunId().equals(baseline)
                ? baselineAvailability != DimensionAvailability.UNAVAILABLE
                : targetAvailability != DimensionAvailability.UNAVAILABLE).toList();
        DimensionAvailability availability = dimensionAvailability(
                baselineAvailability, targetAvailability, all.size());
        if (availability == DimensionAvailability.UNAVAILABLE) return new MetricsComparison(availability, List.of());
        if (baselineAvailability == DimensionAvailability.UNAVAILABLE
                || targetAvailability == DimensionAvailability.UNAVAILABLE) {
            return new MetricsComparison(DimensionAvailability.PARTIAL, List.of());
        }
        Map<String, AnalysisMetric> old = new LinkedHashMap<>(), next = new LinkedHashMap<>();
        all.forEach(metric -> (metric.getRunId().equals(baseline) ? old : next)
                .put(metric.getDimension() + "\u0000" + metric.getMetricKey(), metric));
        Set<String> keys = new java.util.TreeSet<>(); keys.addAll(old.keySet()); keys.addAll(next.keySet());
        List<MetricChange> items = keys.stream().map(key -> {
            AnalysisMetric a = old.get(key), b = next.get(key);
            AnalysisMetric item = b == null ? a : b;
            Change change = a == null ? Change.ADDED : b == null ? Change.REMOVED : Change.RETAINED;
            return new MetricChange(item.getDimension(), item.getMetricKey(),
                    a == null ? null : a.getNumericValue(), b == null ? null : b.getNumericValue(),
                    a == null || b == null ? null : b.getNumericValue().subtract(a.getNumericValue()), change);
        }).toList();
        return new MetricsComparison(availability, items);
    }

    private static FindingsComparison compareFindings(List<AnalysisFinding> all, UUID baseline,
            UUID target, DimensionAvailability baselineAvailability,
            DimensionAvailability targetAvailability) {
        all = all.stream().filter(finding -> finding.getRunId().equals(baseline)
                ? baselineAvailability != DimensionAvailability.UNAVAILABLE
                : targetAvailability != DimensionAvailability.UNAVAILABLE).toList();
        DimensionAvailability availability = dimensionAvailability(
                baselineAvailability, targetAvailability, all.size());
        EnumMap<FindingChange, Long> counts = new EnumMap<>(FindingChange.class);
        EnumMap<FindingChange, Long> unclassifiedCounts = new EnumMap<>(FindingChange.class);
        EnumMap<FindingChange, Map<FindingSeverity, Long>> severityCounts = new EnumMap<>(FindingChange.class);
        for (FindingChange change : FindingChange.values()) {
            counts.put(change, 0L); unclassifiedCounts.put(change, 0L);
            EnumMap<FindingSeverity, Long> m = new EnumMap<>(FindingSeverity.class);
            for (FindingSeverity s : FindingSeverity.values()) m.put(s, 0L);
            severityCounts.put(change, m);
        }
        if (availability == DimensionAvailability.UNAVAILABLE)
            return new FindingsComparison(availability, List.of(), null, null, null);
        if (baselineAvailability == DimensionAvailability.UNAVAILABLE
                || targetAvailability == DimensionAvailability.UNAVAILABLE) {
            return new FindingsComparison(DimensionAvailability.PARTIAL, List.of(),
                    null, null, null);
        }
        Map<String, List<AnalysisFinding>> old = new LinkedHashMap<>(), next = new LinkedHashMap<>();
        List<AnalysisFinding> unmatchedOld = new java.util.ArrayList<>(), unmatchedNew = new java.util.ArrayList<>();
        all.forEach(f -> {
            if (!f.getRunId().equals(baseline) && !f.getRunId().equals(target)) return;
            String fp = f.getFingerprint();
            if (fp == null || fp.isBlank()) (f.getRunId().equals(baseline) ? unmatchedOld : unmatchedNew).add(f);
            else (f.getRunId().equals(baseline) ? old : next).computeIfAbsent(fp, ignored -> new java.util.ArrayList<>()).add(f);
        });
        Set<String> fingerprints = new HashSet<>(); fingerprints.addAll(old.keySet()); fingerprints.addAll(next.keySet());
        List<FindingChangeItem> items = new java.util.ArrayList<>();
        for (String fp : fingerprints) {
            List<AnalysisFinding> a = old.getOrDefault(fp, List.of()), b = next.getOrDefault(fp, List.of());
            if (a.size() == 1 && b.size() == 1) addFinding(items, counts, severityCounts,
                    unclassifiedCounts, FindingChange.PERSISTING, b.getFirst());
            else { a.forEach(f -> addFinding(items, counts, severityCounts, unclassifiedCounts,
                    FindingChange.RESOLVED, f));
                b.forEach(f -> addFinding(items, counts, severityCounts, unclassifiedCounts,
                        FindingChange.ADDED, f)); }
        }
        unmatchedOld.forEach(f -> addFinding(items, counts, severityCounts, unclassifiedCounts,
                FindingChange.RESOLVED, f));
        unmatchedNew.forEach(f -> addFinding(items, counts, severityCounts, unclassifiedCounts,
                FindingChange.ADDED, f));
        items.sort(Comparator.comparing((FindingChangeItem i) -> i.change().name())
                .thenComparing(i -> i.ruleId()).thenComparing(i -> i.fingerprint(), Comparator.nullsFirst(String::compareTo))
                .thenComparing(i -> i.filePath(), Comparator.nullsFirst(String::compareTo)));
        return new FindingsComparison(availability, items, counts, severityCounts, unclassifiedCounts);
    }

    private static void addFinding(List<FindingChangeItem> items, Map<FindingChange, Long> counts,
            Map<FindingChange, Map<FindingSeverity, Long>> severityCounts,
            Map<FindingChange, Long> unclassifiedCounts,
            FindingChange change, AnalysisFinding finding) {
        items.add(FindingChangeItem.of(change, finding)); counts.merge(change, 1L, Long::sum);
        if (finding.getSeverity() != null) severityCounts.get(change).merge(finding.getSeverity(), 1L, Long::sum);
        else unclassifiedCounts.merge(change, 1L, Long::sum);
    }

    private static DimensionAvailability dimensionAvailability(
        DimensionAvailability baseline, DimensionAvailability target, int resultCount) {

    if (baseline == DimensionAvailability.COMPLETE
            && target == DimensionAvailability.COMPLETE) {
        return DimensionAvailability.COMPLETE;
    }

    if (baseline == DimensionAvailability.UNAVAILABLE
            && target == DimensionAvailability.UNAVAILABLE) {
        return DimensionAvailability.UNAVAILABLE;
    }

    return DimensionAvailability.PARTIAL;
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

    @Transactional
    public void failAfterPersistenceError(UUID runId, UUID ownerId) {
        AnalysisRun run = runRepository.findByRunIdAndOwner_UserId(runId, ownerId)
                .orElse(null);
        if (run == null || run.getStatus().isTerminal()) return;
        run.transitionTo(AnalysisRunStatus.FAILED, Instant.now(), null,
                "Analysis failed unexpectedly.");
        runRepository.save(run);
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

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100.");
        }
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
