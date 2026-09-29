package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.persistence.entity.ResolutionEntity;

/** Spring Data repository for {@link com.ramy.bugreport.persistence.entity.ResolutionEntity}; only the built-in CRUD methods are used. */
public interface ResolutionJpaRepository extends JpaRepository<ResolutionEntity, UUID> {
}
