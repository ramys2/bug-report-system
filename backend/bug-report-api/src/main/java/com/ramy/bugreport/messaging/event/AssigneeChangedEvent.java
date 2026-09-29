package com.ramy.bugreport.messaging.event;

/**
 * A developer was assigned to a report ({@code eventType} {@code ASSIGNEE_CHANGED}).
 *
 * @param assigneeName name of the new assignee
 * @param assigneeEmail email of the new assignee; the only recipient of the notification
 * @param reporterName name of the report's reporter; currently not used in the email text
 * @param reportTitle title of the report
 */
public record AssigneeChangedEvent(
		String assigneeName,
		String assigneeEmail,
		String reporterName,
		String reportTitle
		) implements IBugReportEvent {
}
