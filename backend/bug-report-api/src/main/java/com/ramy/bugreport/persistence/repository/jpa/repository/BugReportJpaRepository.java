package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.persistence.entity.BugReportEntity;

public interface BugReportJpaRepository extends JpaRepository<BugReportEntity, UUID> {
    List<BugReportEntity> findByReporterId(UUID reporterId);
    List<BugReportEntity> findByAssigneeId(UUID assigneeId);
    boolean existsByAssigneeIdAndStatusNot(UUID assigneeId, EBugStatus status);
}
