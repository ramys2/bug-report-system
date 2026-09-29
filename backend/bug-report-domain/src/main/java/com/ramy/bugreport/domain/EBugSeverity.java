package com.ramy.bugreport.domain;
/**
 * How serious a {@link BugReport} is, from least to most severe. Stored by name in the database.
 */
public enum EBugSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
