package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.persistence.entity.BugReportEntity;

/** Spring Data repository for {@link com.ramy.bugreport.persistence.entity.BugReportEntity}; the methods below are derived from their names. */
public interface BugReportJpaRepository extends JpaRepository<BugReportEntity, UUID> {
    /** Reports filed by the given user. */
    List<BugReportEntity> findByReporterId(UUID reporterId);
    /** Reports assigned to the given user. */
    List<BugReportEntity> findByAssigneeId(UUID assigneeId);
    /** Whether the user is assigned to any report whose status differs from the given one. */
    boolean existsByAssigneeIdAndStatusNot(UUID assigneeId, EBugStatus status);
}
