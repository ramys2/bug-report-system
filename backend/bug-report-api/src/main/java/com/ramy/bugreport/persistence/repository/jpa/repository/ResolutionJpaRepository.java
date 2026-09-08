package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.persistence.entity.ResolutionEntity;

public interface ResolutionJpaRepository extends JpaRepository<ResolutionEntity, UUID> {
}
