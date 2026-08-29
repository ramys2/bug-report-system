package com.ramy.bugreport.dto.report;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.UserAccount;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BugReportBriefResponse(
        UUID reportId,
        String title,
        String author,
        String assignee,
        String status,
        String severity,
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
                report.getStatus().name().toLowerCase(Locale.ROOT),
                report.getSeverity().name().toLowerCase(Locale.ROOT),
                report.getCreatedAt().format(CREATED_AT_FORMATTER));
    }
}
