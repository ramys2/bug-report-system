package com.ramy.bugreport.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Resolution {

    private UUID id;

    private String description;
    private LocalDateTime resolvedAt;

    private String fixedVersion;

    private String commitUrl;

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
