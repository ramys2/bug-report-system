package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugStatus;

/** Storage contract for {@link BugReport}s. Implemented by a JPA adapter in the API module. */
public interface IBugReportRepository {
    /**
     * Finds one bug report by id.
     *
     * @return the bug report, or empty if none exists
     */
    Optional<BugReport> findById(UUID id);
    /** Returns all stored bug report objects, in no guaranteed order. */
    List<BugReport> findAll();
    /**
     * Inserts the object if its id is {@code null}, otherwise updates the stored object with that id.
     *
     * @param domain the object to store
     * @return the stored object, with its generated id filled in
     */
    BugReport save(BugReport domain);
    /** Returns the number of stored bug report objects. */
    long count();
    /** Removes every stored bug report. Mainly intended for test setup. */
    void deleteAll();
    /** Returns whether a bug report with this id exists. */
    boolean existsById(UUID id);
    /** Returns all reports filed by the given user. */
    List<BugReport> findByReporterId(UUID reporterId);
    /** Returns all reports currently assigned to the given user, regardless of status. */
    List<BugReport> findByAssigneeId(UUID assigneeId);
    /**
     * Returns whether the user is assigned to at least one report whose status differs from {@code status}.
     *
     * Passing {@link EBugStatus#CLOSED} answers "does this user still have unfinished assigned reports?".
     */
    boolean existsByAssigneeIdAndStatusNot(UUID assigneeId, EBugStatus status);
}
