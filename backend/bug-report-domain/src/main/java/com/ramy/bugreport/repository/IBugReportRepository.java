package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugStatus;

public interface IBugReportRepository {
    Optional<BugReport> findById(UUID id);
    List<BugReport> findAll();
    BugReport save(BugReport domain);
    long count();
    void deleteAll();
    boolean existsById(UUID id);
    List<BugReport> findByReporterId(UUID reporterId);
    List<BugReport> findByAssigneeId(UUID assigneeId);
    boolean existsByAssigneeIdAndStatusNot(UUID assigneeId, EBugStatus status);
}
