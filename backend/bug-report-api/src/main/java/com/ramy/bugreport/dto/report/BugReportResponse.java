package com.ramy.bugreport.dto.report;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.Resolution;

public record BugReportResponse(
    UUID id,
    UUID reporterId,
    UUID assigneeId,
    UUID projectId,
    UUID componentId,
    String title,
    String description,
    String stepsToReproduce,
    String expectedBehavior,
    String actualBehavior,
    EBugSeverity severity,
    EBugStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Resolution resolution
) {

    public static BugReportResponse from(BugReport report) {
        return new BugReportResponse(
            report.getId(),
            report.getReporterId(),
            report.getAssigneeId(),
            report.getProjectId(),
            report.getComponentId(),
            report.getTitle(),
            report.getDescription(),
            report.getStepsToReproduce(),
            report.getExpectedBehavior(),
            report.getActualBehavior(),
            report.getSeverity(),
            report.getStatus(),
            report.getCreatedAt(),
            report.getUpdatedAt(),
            report.getResolution()
        );
    }

}
