package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.persistence.entity.SoftwareProjectEntity;

/** Spring Data repository for {@link com.ramy.bugreport.persistence.entity.SoftwareProjectEntity}; only the built-in CRUD methods are used. */
public interface SoftwareProjectJpaRepository extends JpaRepository<SoftwareProjectEntity, UUID> {
}
