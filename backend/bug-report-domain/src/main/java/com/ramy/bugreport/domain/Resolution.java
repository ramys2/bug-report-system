package com.ramy.bugreport.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Describes how a {@link BugReport} was resolved. Attached to a report when it is closed. Immutable.
 */
public class Resolution {

    /**
     * Unique identifier. {@code null} until the resolution is saved.
     */
    private UUID id;

    /**
     * What was done to fix the bug.
     */
    private String description;
    /**
     * When the bug was resolved.
     */
    private LocalDateTime resolvedAt;

    /**
     * Software version that contains the fix. May be {@code null}.
     */
    private String fixedVersion;

    /**
     * URL of the fixing commit. May be {@code null}.
     */
    private String commitUrl;

    /**
     * Creates a new, not yet saved resolution ({@code id} is {@code null}).
     */
    public Resolution(String description, LocalDateTime resolvedAt, String fixedVersion, String commitUrl) {
        this(null, description, resolvedAt, fixedVersion, commitUrl);
    }

    public Resolution(UUID id, String description, LocalDateTime resolvedAt, String fixedVersion, String commitUrl) {
        this.id = id;
        this.description = description;
        this.resolvedAt = resolvedAt;
        this.fixedVersion = fixedVersion;
        this.commitUrl = commitUrl;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getFixedVersion() {
        return fixedVersion;
    }

    public String getCommitUrl() {
        return commitUrl;
    }
}
