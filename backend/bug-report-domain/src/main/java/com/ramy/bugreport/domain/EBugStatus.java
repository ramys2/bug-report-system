package com.ramy.bugreport.domain;
/**
 * Workflow state of a {@link BugReport}. Stored by name in the database.
 *
 * <p>New reports start as {@link #OPEN}, or as {@link #ASSIGNED} if they are created with an
 * assignee. Allowed changes between statuses are defined by {@link #canTransitionTo(EBugStatus)}.
 * {@link #REJECTED} is not part of the lifecycle, so no status can be changed to or from it.
 * {@link #CLOSED} is final.
 */
public enum EBugStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    NEEDS_INFORMATION,
    REVIEWING,
    REJECTED,
    CLOSED;

    /**
     * Tells whether a report in this status may be moved to {@code target}.
     * A status never transitions to itself.
     */
    public boolean canTransitionTo(EBugStatus target) {
        return switch (this) {
            case OPEN -> target == ASSIGNED;
            case ASSIGNED -> target == IN_PROGRESS || target == NEEDS_INFORMATION;
            case IN_PROGRESS -> target == NEEDS_INFORMATION || target == REVIEWING;
            case NEEDS_INFORMATION -> target == IN_PROGRESS;
            case REVIEWING -> target == IN_PROGRESS || target == CLOSED;
            case REJECTED, CLOSED -> false;
        };
    }
}
