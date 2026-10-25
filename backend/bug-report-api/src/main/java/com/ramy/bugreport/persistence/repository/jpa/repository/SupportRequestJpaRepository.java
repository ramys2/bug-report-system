package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.persistence.entity.SupportRequestEntity;

import jakarta.persistence.LockModeType;

/** Spring Data storage for support requests; the database also enforces pending-role uniqueness. */
public interface SupportRequestJpaRepository extends JpaRepository<SupportRequestEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SupportRequestEntity s where s.id = :id")
    Optional<SupportRequestEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByRequesterIdAndTypeAndRequestedRoleAndStatus(UUID requesterId, ESupportRequestType type,
            EUserRole requestedRole, ESupportRequestStatus status);
}
