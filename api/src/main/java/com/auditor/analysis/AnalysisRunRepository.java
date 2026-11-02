package com.auditor.analysis;

import com.auditor.analysis.dto.AnalysisRunHistoryItemResponse;
import com.auditor.analysis.dto.AnalysisReportRunOverview;
import com.auditor.analysis.dto.AnalysisRunComparisonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisRunRepository
        extends Repository<AnalysisRun, UUID> {

    Optional<AnalysisRun> findByRunIdAndOwner_UserId(
            UUID runId,
            UUID ownerUserId);

    @Query("""
            select new com.auditor.analysis.dto.AnalysisReportRunOverview(
                run.runId,
                run.submission.submissionId,
                run.status,
                run.score,
                run.grade,
                run.createdAt,
                run.startedAt,
                run.completedAt,
                run.errorSummary)
            from AnalysisRun run
            where run.runId = :runId
                and run.submission.submissionId = :submissionId
                and run.owner.userId = :ownerId
            """)
    Optional<AnalysisReportRunOverview> findReportOverview(
            @Param("runId") UUID runId,
            @Param("submissionId") UUID submissionId,
            @Param("ownerId") UUID ownerId);

    @Query("""
            select new com.auditor.analysis.dto.AnalysisRunComparisonResponse$RunOverview(
                run.runId, run.status, run.score, run.grade, run.createdAt, run.completedAt)
            from AnalysisRun run
            where run.runId in :runIds and run.submission.submissionId = :submissionId
                and run.owner.userId = :ownerId
            """)
    java.util.List<AnalysisRunComparisonResponse.RunOverview> findComparisonRuns(
            @Param("runIds") java.util.List<UUID> runIds,
            @Param("submissionId") UUID submissionId,
            @Param("ownerId") UUID ownerId);

    @Query(
            value = """
                    select new com.auditor.analysis.dto.AnalysisRunHistoryItemResponse(
                        run.runId,
                        run.submission.submissionId,
                        run.status,
                        run.score,
                        run.grade,
                        run.createdAt,
                        run.startedAt,
                        run.completedAt,
                        run.errorSummary)
                    from AnalysisRun run
                    where run.submission.submissionId = :submissionId
                        and run.owner.userId = :ownerId
                    order by run.createdAt desc, run.runId desc
                    """,
            countQuery = """
                    select count(run)
                    from AnalysisRun run
                    where run.submission.submissionId = :submissionId
                        and run.owner.userId = :ownerId
                    """)
    Page<AnalysisRunHistoryItemResponse> findHistoryBySubmissionAndOwner(
            @Param("submissionId") UUID submissionId,
            @Param("ownerId") UUID ownerId,
            Pageable pageable);

    <S extends AnalysisRun> S save(S run);

    <S extends AnalysisRun> S saveAndFlush(S run);
}
