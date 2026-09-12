package com.ramy.bugreport.messaging.event;

public record AssigneeChangedEvent(
		String assigneeName,
		String assigneeEmail,
		String reportTitle
		) {
}
