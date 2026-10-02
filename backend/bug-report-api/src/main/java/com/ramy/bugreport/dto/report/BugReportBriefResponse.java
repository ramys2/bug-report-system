package com.ramy.bugreport.dto.report;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;
/**
 * A report in the list endpoints ({@code GET /api/reports}, {@code /reported}, {@code /assigned}).
 *
 * @param reportId report id
 * @param title short summary
 * @param author display name of the reporter
 * @param assignee display name of the assignee; null if unassigned
 * @param status workflow status
 * @param severity severity
 * @param createdAt creation time (ISO-8601)
 */
@Schema(description = "A report in the list endpoints (`GET /api/reports`, `/reported`, `/assigned`).")
public record BugReportBriefResponse(
        @Schema(description = "Report id.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID reportId,
        @Schema(description = "Short summary.", example = "Valid users cannot sign in", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,
        @Schema(description = "Display name of the reporter.", example = "Rachel Reporter", requiredMode = Schema.RequiredMode.REQUIRED)
        String author,
        @Schema(description = "Display name of the assignee. Null if unassigned.", example = "Daniel Developer")
        String assignee,
        @Schema(description = "Workflow status.", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
        EBugStatus status,
        @Schema(description = "Severity.", example = "CRITICAL", requiredMode = Schema.RequiredMode.REQUIRED)
        EBugSeverity severity,
        @Schema(description = "Creation time (ISO-8601).", example = "2026-05-14T09:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdAt
) {
    public static BugReportBriefResponse from(
            BugReport report,
            UserAccount reporter,
            UserAccount assignee
    ) {
        return new BugReportBriefResponse(
                report.getId(),
                report.getTitle(),
                reporter.getName(),
                assignee == null ? null : assignee.getName(),
                report.getStatus(),
                report.getSeverity(),
                report.getCreatedAt());
    }
}
