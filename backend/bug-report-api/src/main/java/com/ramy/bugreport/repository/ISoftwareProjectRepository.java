package com.ramy.bugreport.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.SoftwareProject;

public interface ISoftwareProjectRepository extends JpaRepository<SoftwareProject, UUID> {
    // No further implementation needed for now
}
