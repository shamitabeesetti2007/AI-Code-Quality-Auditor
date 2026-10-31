package com.auditor.submission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface CodeSubmissionRepository
        extends Repository<CodeSubmission, UUID> {

    Page<CodeSubmission> findAllByOwner_UserId(UUID ownerUserId, Pageable pageable);

    Optional<CodeSubmission> findBySubmissionIdAndOwner_UserId(
            UUID submissionId,
            UUID ownerUserId);

    <S extends CodeSubmission> S save(S submission);

    <S extends CodeSubmission> S saveAndFlush(S submission);

    void delete(CodeSubmission submission);
}
