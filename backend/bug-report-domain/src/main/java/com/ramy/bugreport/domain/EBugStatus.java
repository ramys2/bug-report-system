package com.ramy.bugreport.domain;
/**
 * Workflow state of a {@link BugReport}. Stored by name in the database.
 *
 * <p>New reports start as {@link #OPEN}. This enum itself defines no transition rules.
 */
public enum EBugStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    NEEDS_INFORMATION,
    REVIEWING,
    REJECTED,
    CLOSED
}
