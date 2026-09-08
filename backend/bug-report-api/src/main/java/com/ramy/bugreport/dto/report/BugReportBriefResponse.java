package com.ramy.bugreport.dto.report;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.UserAccount;
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
