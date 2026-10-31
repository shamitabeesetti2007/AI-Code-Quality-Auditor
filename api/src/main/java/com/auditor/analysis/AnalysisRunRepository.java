package com.auditor.analysis;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisRunRepository
        extends Repository<AnalysisRun, UUID> {

    Optional<AnalysisRun> findByRunIdAndOwner_UserId(
            UUID runId,
            UUID ownerUserId);

    List<AnalysisRun> findAllBySubmission_SubmissionIdAndOwner_UserIdOrderByCreatedAtDesc(
            UUID submissionId,
            UUID ownerUserId);

    <S extends AnalysisRun> S save(S run);

    <S extends AnalysisRun> S saveAndFlush(S run);
}
