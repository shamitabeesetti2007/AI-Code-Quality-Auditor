package com.auditor.analysis;

import com.auditor.submission.CodeSubmission;
import com.auditor.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "analysis_run")
public class AnalysisRun {

    private static final Map<AnalysisRunStatus, Set<AnalysisRunStatus>>
            ALLOWED_TRANSITIONS = Map.of(
                    AnalysisRunStatus.QUEUED,
                    Set.of(AnalysisRunStatus.PARSING, AnalysisRunStatus.FAILED),
                    AnalysisRunStatus.PARSING,
                    Set.of(
                            AnalysisRunStatus.ANALYZING,
                            AnalysisRunStatus.PARSE_FAILED,
                            AnalysisRunStatus.FAILED),
                    AnalysisRunStatus.ANALYZING,
                    Set.of(
                            AnalysisRunStatus.SCORING,
                            AnalysisRunStatus.PARTIAL,
                            AnalysisRunStatus.FAILED),
                    AnalysisRunStatus.SCORING,
                    Set.of(
                            AnalysisRunStatus.COMPLETED,
                            AnalysisRunStatus.PARTIAL,
                            AnalysisRunStatus.FAILED));

    @Id
    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, updatable = false)
    private CodeSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false, updatable = false)
    private AppUser owner;

    @Column(
            name = "source_snapshot",
            nullable = true,
            updatable = false,
            columnDefinition = "TEXT")
    private String sourceSnapshot;

    @Column(
            name = "source_hash",
            nullable = true,
            updatable = false,
            length = 64,
            columnDefinition = "CHAR(64)")
    private String sourceHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AnalysisRunStatus status;

    @Column(name = "analyzer_version", length = 80)
    private String analyzerVersion;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "score")
    private BigDecimal score;

    @Column(name = "grade", length = 5)
    private String grade;

    protected AnalysisRun() {
    }

    public AnalysisRun(
            CodeSubmission submission,
            AppUser owner,
            String sourceSnapshot,
            String sourceHash) {
        if (sourceSnapshot == null || sourceSnapshot.isBlank()) {
            throw new IllegalArgumentException(
                    "Analysis source snapshot must not be blank.");
        }
        if (sourceHash == null || !sourceHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Analysis source hash must be a SHA-256 hex digest.");
        }
        this.runId = UUID.randomUUID();
        this.submission = submission;
        this.owner = owner;
        this.sourceSnapshot = sourceSnapshot;
        this.sourceHash = sourceHash;
        this.status = AnalysisRunStatus.QUEUED;
    }

    @PrePersist
    protected void onCreate() {
        if (runId == null) {
            runId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void transitionTo(
            AnalysisRunStatus nextStatus,
            Instant at,
            String analyzerVersion,
            String errorSummary) {
        if (nextStatus == null
                || !ALLOWED_TRANSITIONS.getOrDefault(status, Set.of())
                        .contains(nextStatus)) {
            throw new IllegalStateException(
                    "Invalid analysis run transition from " + status
                            + " to " + nextStatus + ".");
        }
        if (at == null) {
            throw new IllegalArgumentException("Transition time is required.");
        }
        if (startedAt == null) {
            startedAt = at;
        }
        status = nextStatus;
        if (analyzerVersion != null) {
            this.analyzerVersion = analyzerVersion;
        }
        this.errorSummary = errorSummary;
        if (nextStatus.isTerminal()) {
            completedAt = at;
        }
    }

    public void assignScoreAndGrade(BigDecimal score, String grade) {
        if (status != AnalysisRunStatus.SCORING) {
            throw new IllegalStateException(
                    "A score can only be assigned while a run is scoring.");
        }
        if (score == null
                || score.signum() < 0
                || score.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Analysis score must be between 0 and 100.");
        }
        if (grade == null
                || !Set.of("A", "B", "C", "D", "F").contains(grade)) {
            throw new IllegalArgumentException("Analysis grade is invalid.");
        }
        this.score = score;
        this.grade = grade;
    }

    public UUID getRunId() {
        return runId;
    }

    public UUID getSubmissionId() {
        return submission.getSubmissionId();
    }

    public UUID getOwnerUserId() {
        return owner.getUserId();
    }

    public String getSourceSnapshot() {
        return sourceSnapshot;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public AnalysisRunStatus getStatus() {
        return status;
    }

    public String getAnalyzerVersion() {
        return analyzerVersion;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getGrade() {
        return grade;
    }
}
