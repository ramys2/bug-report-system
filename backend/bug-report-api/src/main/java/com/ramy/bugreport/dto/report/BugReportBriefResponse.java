package com.ramy.bugreport.dto.report;

import java.time.format.DateTimeFormatter;
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
 * @param createdAt creation time formatted as {@code dd-MM-yyyy HH:mm} (a string, unlike other timestamps)
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
        @Schema(description = "Creation time formatted as `dd-MM-yyyy HH:mm` (a string, unlike other timestamps).", example = "14-05-2026 09:30", requiredMode = Schema.RequiredMode.REQUIRED)
        String createdAt
) {
    private static final DateTimeFormatter CREATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

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
                report.getCreatedAt().format(CREATED_AT_FORMATTER));
    }
}
