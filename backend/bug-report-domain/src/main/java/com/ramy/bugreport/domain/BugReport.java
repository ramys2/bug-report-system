package com.ramy.bugreport.domain;
import java.time.LocalDateTime;
import java.util.UUID;

public class BugReport {
    private UUID id;
    private UUID reporterId;
    private UUID assigneeId;
    private UUID projectId;
    private UUID componentId; 
    private String title;
    private String description;
    private String stepsToReproduce;
    private String expectedBehavior;
    private String actualBehavior;
    private EBugSeverity severity;
    private EBugStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Resolution resolution;

    public BugReport(UUID id, UUID reporterId, UUID assigneeId, UUID projectId, UUID componentId, String title, String description, String stepsToReproduce, String expectedBehavior, String actualBehavior, EBugSeverity severity, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reporterId = reporterId;
        this.assigneeId = assigneeId;
        this.projectId = projectId;
        this.componentId = componentId;
        this.title = title;
        this.description = description;
        this.stepsToReproduce = stepsToReproduce;
        this.expectedBehavior = expectedBehavior;
        this.actualBehavior = actualBehavior;
        this.severity = severity;
        this.status = EBugStatus.OPEN; // Set the initial status to OPEN
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolution = null;
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
}
