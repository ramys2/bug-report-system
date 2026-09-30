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
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;
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
@Schema(description = "Full detail of one report, returned by `GET /api/reports/{reportId}`.")
public record BugReportResponse(
        @Schema(description = "Report id.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Display name of the reporter.", example = "Rachel Reporter", requiredMode = Schema.RequiredMode.REQUIRED)
        String reporterName,
        @Schema(description = "Display name of the assignee. Null if unassigned.", example = "Daniel Developer")
        String assigneeName,
        @Schema(description = "Name of the project.", example = "Bug Report System", requiredMode = Schema.RequiredMode.REQUIRED)
        String projectName,
        @Schema(description = "Name of the component.", example = "Backend API", requiredMode = Schema.RequiredMode.REQUIRED)
        String componentName,
        @Schema(description = "Short summary.", example = "Valid users cannot sign in", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,
        @Schema(description = "Description. May be null.", example = "The sign-in endpoint returns HTTP 500 for valid credentials.")
        String description,
        @Schema(description = "Steps to reproduce. May be null.", example = "Open sign in, enter valid credentials, and submit the form.")
        String stepsToReproduce,
        @Schema(description = "Expected behavior. May be null.", example = "The user is authenticated and redirected to the dashboard.")
        String expectedBehavior,
        @Schema(description = "Actual behavior. May be null.", example = "The server returns HTTP 500 and the user remains signed out.")
        String actualBehavior,
        @Schema(description = "Severity.", example = "CRITICAL", requiredMode = Schema.RequiredMode.REQUIRED)
        EBugSeverity severity,
        @Schema(description = "Workflow status.", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
        EBugStatus status,
        @Schema(description = "Creation time (ISO-8601).", example = ApiExamples.DATE_TIME, requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdAt,
        @Schema(description = "Last modification time (ISO-8601).", example = ApiExamples.DATE_TIME, requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime updatedAt,
        @Schema(description = "The domain Resolution, serialized directly. Null until the report is closed.")
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
