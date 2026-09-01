package com.ramy.bugreport.dto.report;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.Resolution;
import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.domain.UserAccount;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BugReportResponse(
    UUID id,
    String reporterName,
    String assigneeName,
    String projectName,
    String componentName,
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

    public static BugReportResponse from(
            BugReport report,
            UserAccount reporter,
            UserAccount assignee,
            SoftwareProject project,
            Component component
    ) {
        return new BugReportResponse(
            report.getId(),
            reporter.getName(),
            assignee == null ? null : assignee.getName(),
            project.getName(),
            component.getName(),
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
