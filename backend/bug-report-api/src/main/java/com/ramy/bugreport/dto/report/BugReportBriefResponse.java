package com.ramy.bugreport.dto.report;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.UserAccount;
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
public record BugReportBriefResponse(
        UUID reportId,
        String title,
        String author,
        String assignee,
        EBugStatus status,
        EBugSeverity severity,
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
