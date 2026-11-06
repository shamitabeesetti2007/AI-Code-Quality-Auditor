
package com.auditor.auth.token;

import com.auditor.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @Column(name = "refresh_token_id", nullable = false, updatable = false)
    private UUID refreshTokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
    private byte[] tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "family_expires_at", nullable = false, updatable = false)
    private Instant familyExpiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "revocation_reason", length = 40)
    private String revocationReason;

    protected RefreshToken() {
        // Required by JPA.
    }

    public RefreshToken(
            UUID refreshTokenId,
            AppUser user,
            UUID familyId,
            byte[] tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            Instant familyExpiresAt) {
        this.refreshTokenId = refreshTokenId;
        this.user = user;
        this.familyId = familyId;
        this.tokenHash = tokenHash.clone();
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.familyExpiresAt = familyExpiresAt;
    }

    public UUID getRefreshTokenId() {
        return refreshTokenId;
    }

    public AppUser getUser() {
        return user;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public byte[] getTokenHash() {
        return tokenHash.clone();
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getFamilyExpiresAt() {
        return familyExpiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void consume(Instant at, UUID successorId) {
        if (usedAt != null || replacedBy != null) {
            throw new IllegalStateException("Refresh token already consumed.");
        }
        if (successorId == null || successorId.equals(refreshTokenId)) {
            throw new IllegalArgumentException("Invalid successor token ID.");
        }
        this.usedAt = at;
        this.replacedBy = successorId;
    }

    public void revoke(Instant at, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Revocation reason is required.");
        }
        if (revokedAt == null) {
            this.revokedAt = at;
            this.revocationReason = reason;
        }
    }
}

