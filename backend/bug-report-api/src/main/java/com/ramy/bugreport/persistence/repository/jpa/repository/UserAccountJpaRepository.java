package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.persistence.entity.UserAccountEntity;

import jakarta.persistence.LockModeType;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, UUID> {
    Optional<UserAccountEntity> findByEmailAddress(String emailAddress);
    boolean existsByEmailAddress(String emailAddress);
    List<UserAccountEntity> findByNameContainingIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<UserAccountEntity> findAllByRole(EUserRole role);
}
