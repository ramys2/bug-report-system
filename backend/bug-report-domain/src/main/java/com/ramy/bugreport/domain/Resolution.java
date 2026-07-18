package com.ramy.bugreport.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Resolution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String description;
    private LocalDateTime resolvedAt;

    private String fixedVersion;

    private String commitUrl;

    protected Resolution() {
        // Required by JPA
    }

    public Resolution(String description, LocalDateTime resolvedAt, String fixedVersion, String commitUrl) {
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
