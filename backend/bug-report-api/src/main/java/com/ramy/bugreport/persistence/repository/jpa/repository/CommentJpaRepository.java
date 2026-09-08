package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.persistence.entity.CommentEntity;

public interface CommentJpaRepository extends JpaRepository<CommentEntity, UUID> {
    List<CommentEntity> findByBugReportId(UUID bugReportId);
}
