package com.ramy.bugreport.persistence.repository.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.persistence.entity.SoftwareProjectEntity;

public interface SoftwareProjectJpaRepository extends JpaRepository<SoftwareProjectEntity, UUID> {
}
