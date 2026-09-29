package com.ramy.bugreport.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A bug report: the central domain object of the system.
 *
 * References other aggregates ({@link UserAccount}, {@link SoftwareProject}, {@link Component})
 * by id rather than by object. Instances are created through {@link #builder}.
 */
public class BugReport {

    /**
     * Unique identifier. {@code null} until the report is saved.
     */
    private UUID id;

    /**
     * Id of the {@link UserAccount} that filed the report. Required.
     */
    private UUID reporterId;

    /**
     * Id of the {@link UserAccount} working on the report. {@code null} while unassigned.
     */
    private UUID assigneeId;

    /**
     * Id of the {@link SoftwareProject} the bug belongs to. Required.
     */
    private UUID projectId;

    /**
     * Id of the {@link Component} the bug belongs to. Required.
     */
    private UUID componentId;

    /**
     * Short summary of the bug. Required.
     */
    private String title;

    /** Free-text description of the bug. Optional. */
    private String description;

    /** How to reproduce the bug. Optional. */
    private String stepsToReproduce;

    /** What the reporter expected to happen. Optional. */
    private String expectedBehavior;

    /** What actually happened. Optional. */
    private String actualBehavior;

    /**
     * How serious the bug is. Required.
     */
    private EBugSeverity severity;

    /**
     * Current workflow state. Always {@link EBugStatus#OPEN} on newly built reports.
     */
    private EBugStatus status;

    /** When the report was created. Set by the caller; this class never updates it. */
    private LocalDateTime createdAt;

    /** When the report was last modified. Set by the caller; this class never updates it. */
    private LocalDateTime updatedAt;

    /**
     * How the bug was fixed. {@code null} until the report is closed with a resolution.
     */
    private Resolution resolution;

    /**
     * Copies the builder's values. The status is always set to {@link EBugStatus#OPEN};
     * the builder cannot set a status, the id or a resolution.
     */
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

    /**
     * Starts building a new report with its required fields.
     *
     * @param reporterId id of the reporting user
     * @param projectId id of the project
     * @param componentId id of the component
     * @param title short summary
     * @param severity severity of the bug
     * @return a builder for setting the optional fields
     * @throws NullPointerException if any argument is {@code null}
     */
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

    // The setters below perform no validation; they are used to update a report and to rebuild it from the database.
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

    /**
     * Builder for {@link BugReport}. Required fields are passed to {@link BugReport#builder};
     * all other fields are optional and default to {@code null}.
     */
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

        /**
         * Creates the report with status {@link EBugStatus#OPEN}.
         */
        public BugReport build() {
            return new BugReport(this);
        }
    }
}
