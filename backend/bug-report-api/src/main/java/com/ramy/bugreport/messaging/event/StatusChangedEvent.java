package com.ramy.bugreport.messaging.event;

/**
 * The status of a report changed ({@code eventType} {@code STATUS_CHANGED}).
 *
 * @param reportTitle title of the report
 * @param assigneeEmail email of the assignee; {@code null} if unassigned
 * @param reporterEmail email of the reporter; both emails are recipients of the notification
 */
public record StatusChangedEvent(
		String reportTitle,
		String assigneeEmail,
		String reporterEmail
		) implements IBugReportEvent {
}
