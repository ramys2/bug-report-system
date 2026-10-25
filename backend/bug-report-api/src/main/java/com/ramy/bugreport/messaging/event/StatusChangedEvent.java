package com.ramy.bugreport.messaging.event;

import java.util.UUID;

/**
 * The status of a report changed ({@code eventType} {@code STATUS_CHANGED}).
 *
 * @param reportId identifier of the report
 * @param reportTitle title of the report
 * @param assigneeEmail email of the assignee; {@code null} if unassigned
 * @param reporterEmail email of the reporter; both emails are recipients of the notification
 */
public record StatusChangedEvent(
		UUID reportId,
		String reportTitle,
		String assigneeEmail,
		String reporterEmail
		) implements IBugReportEvent {
}
