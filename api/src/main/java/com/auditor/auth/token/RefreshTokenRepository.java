
package com.auditor.auth.token;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    /**
     * Retrieves only the family ID. This does not load a RefreshToken
     * entity into the current persistence context before family locking.
     */
    @Query("""
        select token.familyId
        from RefreshToken token
        where token.tokenHash = :tokenHash
        """)
    Optional<UUID> findFamilyIdByTokenHash(
            @Param("tokenHash") byte[] tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from RefreshToken token
        where token.tokenHash = :tokenHash
        """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") byte[] tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from RefreshToken token
        where token.familyId = :familyId
        order by token.refreshTokenId
        """)
    List<RefreshToken> findFamilyForUpdate(
            @Param("familyId") UUID familyId);
}

