
package com.auditor.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appUser from AppUser appUser where appUser.userId = :userId")
    Optional<AppUser> findByIdForUpdate(@Param("userId") UUID userId);

    long countByRoleAndAccountStatus(
            AppUser.Role role,
            AppUser.AccountStatus accountStatus);

    long countByRole(AppUser.Role role);
}
