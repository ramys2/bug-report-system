package com.ramy.bugreport.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.Resolution;

public interface IResolutionRepository extends JpaRepository<Resolution, UUID> {
}
