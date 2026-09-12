package com.ramy.bugreport.messaging.event;

public sealed interface IBugReportEvent permits AssigneeChangedEvent, StatusChangedEvent, BugReportClosedEvent {
	String reportTitle();
}
