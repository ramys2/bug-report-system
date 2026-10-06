package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.persistence.entity.ComponentEntity;

/** Spring Data repository for {@link com.ramy.bugreport.persistence.entity.ComponentEntity}: the built-in CRUD methods plus a derived query by project. */
public interface ComponentJpaRepository extends JpaRepository<ComponentEntity, UUID> {

    /** Spring Data derives the query from the method name: {@code WHERE project_id = ?}. */
    List<ComponentEntity> findByProjectId(UUID projectId);
}
