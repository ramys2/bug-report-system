package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.persistence.entity.UserAccountEntity;

import jakarta.persistence.LockModeType;

/** Spring Data repository for {@link com.ramy.bugreport.persistence.entity.UserAccountEntity}. */
public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, UUID> {
    /** Account with exactly this email address (comparison is left to the database collation). */
    Optional<UserAccountEntity> findByEmailAddress(String emailAddress);
    /** Whether an account with exactly this email address exists. */
    boolean existsByEmailAddress(String emailAddress);
    /** Accounts whose name contains the text, ignoring case. */
    List<UserAccountEntity> findByNameContainingIgnoreCase(String name);

    /**
     * Accounts with the given role, read with a pessimistic write lock (held until the surrounding transaction ends),
     * which lets callers serialize concurrent changes such as demoting admins.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<UserAccountEntity> findAllByRole(EUserRole role);
}
