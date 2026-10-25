package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.persistence.entity.SupportRequestEntity;

/** Spring Data storage for support requests; the database also enforces pending-role uniqueness. */
public interface SupportRequestJpaRepository extends JpaRepository<SupportRequestEntity, UUID> {
    boolean existsByRequesterIdAndTypeAndRequestedRoleAndStatus(UUID requesterId, ESupportRequestType type,
            EUserRole requestedRole, ESupportRequestStatus status);
}
