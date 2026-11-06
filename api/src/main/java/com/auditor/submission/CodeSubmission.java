package com.auditor.submission;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_submission")
public class CodeSubmission {

    @Id
    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false, updatable = false)
    private AppUser owner;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 30)
    private SubmissionLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 30)
    private SubmissionOriginType originType;

    @Column(name = "provenance_verified", nullable = false)
    private boolean provenanceVerified;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(
            name = "content_hash",
            nullable = false,
            length = 64,
            columnDefinition = "CHAR(64)")
    private String contentHash;

    @Column(name = "logical_loc", nullable = false)
    private int logicalLoc;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CodeSubmission() {
    }

    public CodeSubmission(
            AppUser owner,
            String title,
            SubmissionLanguage language,
            SubmissionOriginType originType,
            String sourceCode,
            String contentHash,
            int logicalLoc) {
        this.submissionId = UUID.randomUUID();
        this.owner = owner;
        this.title = title;
        this.language = language;
        this.originType = originType;
        this.provenanceVerified = false;
        this.sourceCode = sourceCode;
        this.contentHash = contentHash;
        this.logicalLoc = logicalLoc;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (submissionId == null) {
            submissionId = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateContent(
            String title,
            String sourceCode,
            String contentHash,
            int logicalLoc) {
        this.title = title;
        this.sourceCode = sourceCode;
        this.contentHash = contentHash;
        this.logicalLoc = logicalLoc;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public UUID getOwnerUserId() {
        return owner.getUserId();
    }

    public AppUser getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public SubmissionLanguage getLanguage() {
        return language;
    }

    public SubmissionOriginType getOriginType() {
        return originType;
    }

    public boolean isProvenanceVerified() {
        return provenanceVerified;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getContentHash() {
        return contentHash;
    }

    public int getLogicalLoc() {
        return logicalLoc;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "CodeSubmission[submissionId=" + submissionId
                + ", ownerUserId=" + getOwnerUserId()
                + ", language=" + language
                + ", contentHash=" + contentHash + "]";
    }
}
