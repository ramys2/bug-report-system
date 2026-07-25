package com.ramy.bugreport.domain;

import jakarta.persistence.CascadeType;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class BugReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID reporterId;

    private UUID assigneeId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID componentId;

    @Column(nullable = false)
    private String title;

    private String description;
    private String stepsToReproduce;
    private String expectedBehavior;
    private String actualBehavior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EBugSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EBugStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "resolution_id", unique = true)
    private Resolution resolution;

    protected BugReport() {
        // Required by JPA
    }

    private BugReport(Builder builder) {
        reporterId = builder.reporterId;
        assigneeId = builder.assigneeId;
        projectId = builder.projectId;
        componentId = builder.componentId;
        title = builder.title;
        description = builder.description;
        stepsToReproduce = builder.stepsToReproduce;
        expectedBehavior = builder.expectedBehavior;
        actualBehavior = builder.actualBehavior;
        severity = builder.severity;
        status = EBugStatus.OPEN;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
    }

    public static Builder builder(UUID reporterId, UUID projectId, UUID componentId, String title, EBugSeverity severity) {
        return new Builder(reporterId, projectId, componentId, title, severity);
    }

    public UUID getId() {
        return id;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getComponentId() {
        return componentId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStepsToReproduce() {
        return stepsToReproduce;
    }

    public String getExpectedBehavior() {
        return expectedBehavior;
    }

    public String getActualBehavior() {
        return actualBehavior;
    }

    public EBugSeverity getSeverity() {
        return severity;
    }

    public EBugStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public void setStatus(EBugStatus status) {
        this.status = status;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setReporterId(UUID reporterId) {
        this.reporterId = reporterId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public void setComponentId(UUID componentId) {
        this.componentId = componentId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStepsToReproduce(String stepsToReproduce) {
        this.stepsToReproduce = stepsToReproduce;
    }

    public void setExpectedBehavior(String expectedBehavior) {
        this.expectedBehavior = expectedBehavior;
    }

    public void setActualBehavior(String actualBehavior) {
        this.actualBehavior = actualBehavior;
    }

    public void setSeverity(EBugSeverity severity) {
        this.severity = severity;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static final class Builder {
        private final UUID reporterId;
        private final UUID projectId;
        private final UUID componentId;
        private final String title;
        private final EBugSeverity severity;
        private UUID assigneeId;
        private String description;
        private String stepsToReproduce;
        private String expectedBehavior;
        private String actualBehavior;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder(UUID reporterId, UUID projectId, UUID componentId, String title, EBugSeverity severity) {
            this.reporterId = Objects.requireNonNull(reporterId, "reporterId is required");
            this.projectId = Objects.requireNonNull(projectId, "projectId is required");
            this.componentId = Objects.requireNonNull(componentId, "componentId is required");
            this.title = Objects.requireNonNull(title, "title is required");
            this.severity = Objects.requireNonNull(severity, "severity is required");
        }

        public Builder assigneeId(UUID assigneeId) {
            this.assigneeId = assigneeId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder stepsToReproduce(String stepsToReproduce) {
            this.stepsToReproduce = stepsToReproduce;
            return this;
        }

        public Builder expectedBehavior(String expectedBehavior) {
            this.expectedBehavior = expectedBehavior;
            return this;
        }

        public Builder actualBehavior(String actualBehavior) {
            this.actualBehavior = actualBehavior;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public BugReport build() {
            return new BugReport(this);
        }
    }
}
