package com.ramy.bugreport.messaging.event;

import java.util.UUID;

/**
 * A developer was assigned to a report ({@code eventType} {@code ASSIGNEE_CHANGED}).
 *
 * @param reportId identifier of the report
 * @param assigneeName name of the new assignee
 * @param assigneeEmail email of the new assignee; the only recipient of the notification
 * @param reportTitle title of the report
 */
public record AssigneeChangedEvent(
		UUID reportId,
		String assigneeName,
		String assigneeEmail,
		String reportTitle
		) implements IBugReportEvent {
}
