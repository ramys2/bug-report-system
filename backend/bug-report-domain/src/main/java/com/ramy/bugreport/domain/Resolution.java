package com.ramy.bugreport.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Resolution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bugReportId;

    private String description;
    private LocalDateTime resolvedAt;

    private String fixedVersion;

    private String commitUrl;

    protected Resolution() {
        // Required by JPA
    }

    public Resolution(UUID bugReportId, String description, LocalDateTime resolvedAt, String fixedVersion, String commitUrl) {
        this.bugReportId = bugReportId;
        this.description = description;
        this.resolvedAt = resolvedAt;
        this.fixedVersion = fixedVersion;
        this.commitUrl = commitUrl;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBugReportId() {
        return bugReportId;
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
