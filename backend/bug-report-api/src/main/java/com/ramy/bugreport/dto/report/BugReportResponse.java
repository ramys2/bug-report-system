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
/**
 * Full detail of one report, returned by {@code GET /api/reports/{reportId}}.
 *
 * @param id report id
 * @param reporterName display name of the reporter
 * @param assigneeName display name of the assignee; null if unassigned
 * @param projectName name of the project
 * @param componentName name of the component
 * @param title short summary
 * @param description description; may be null
 * @param stepsToReproduce steps to reproduce; may be null
 * @param expectedBehavior expected behavior; may be null
 * @param actualBehavior actual behavior; may be null
 * @param severity severity
 * @param status workflow status
 * @param createdAt creation time (ISO-8601)
 * @param updatedAt last modification time (ISO-8601)
 * @param resolution the domain {@link com.ramy.bugreport.domain.Resolution}, serialized directly; null until the report is closed
 */
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
