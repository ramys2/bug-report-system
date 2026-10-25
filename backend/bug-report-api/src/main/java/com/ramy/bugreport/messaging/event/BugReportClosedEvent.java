package com.ramy.bugreport.messaging.event;

import java.util.UUID;

/**
 * A report was closed with a resolution ({@code eventType} {@code BUG_REPORT_CLOSED}).
 *
 * @param reportId identifier of the report
 * @param reportTitle title of the report
 * @param assigneeEmail email of the assignee; {@code null} if unassigned
 * @param reporterEmail email of the reporter; both emails are recipients of the notification
 */
public record BugReportClosedEvent(
		UUID reportId,
		String reportTitle,
		String assigneeEmail,
		String reporterEmail
		) implements IBugReportEvent {
}
