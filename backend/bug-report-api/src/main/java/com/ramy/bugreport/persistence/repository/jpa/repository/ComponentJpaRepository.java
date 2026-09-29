package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.persistence.entity.ComponentEntity;

/** Spring Data repository for {@link com.ramy.bugreport.persistence.entity.ComponentEntity}; only the built-in CRUD methods are used. */
public interface ComponentJpaRepository extends JpaRepository<ComponentEntity, UUID> {
}
