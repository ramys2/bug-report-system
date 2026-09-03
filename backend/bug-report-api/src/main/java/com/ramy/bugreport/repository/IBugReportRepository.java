package com.ramy.bugreport.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugStatus;
import java.util.List;

public interface IBugReportRepository extends JpaRepository<BugReport, UUID> {
    List<BugReport> findByReporterId(UUID repoterId);
    
    List<BugReport> findByAssigneeId(UUID assigneeId);

    boolean existsByAssigneeIdAndStatusNot(UUID assigneeId, EBugStatus status);
}
