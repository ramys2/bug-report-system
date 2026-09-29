package com.ramy.bugreport.messaging.event;

/**
 * A report was closed with a resolution ({@code eventType} {@code BUG_REPORT_CLOSED}).
 *
 * @param reportTitle title of the report
 * @param assigneeEmail email of the assignee; {@code null} if unassigned
 * @param reporterEmail email of the reporter; both emails are recipients of the notification
 */
public record BugReportClosedEvent(
		String reportTitle,
		String assigneeEmail,
		String reporterEmail
		) implements IBugReportEvent {
}
