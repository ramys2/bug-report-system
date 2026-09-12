package com.ramy.bugreport.messaging.event;

public record StatusChangedEvent(
		String reportTitle,
		String assigneeEmail,
		String reporterEmail
		) implements IBugReportEvent {
}
