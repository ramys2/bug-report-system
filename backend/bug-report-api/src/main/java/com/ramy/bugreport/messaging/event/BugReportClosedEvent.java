package com.ramy.bugreport.messaging.event;

public record BugReportClosedEvent(
		String reportTitle,
		String assigneeEmail,
		String reporterEmail
		) implements IBugReportEvent {
}
