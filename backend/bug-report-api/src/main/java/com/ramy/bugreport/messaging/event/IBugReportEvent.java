package com.ramy.bugreport.messaging.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
		@JsonSubTypes.Type(value = AssigneeChangedEvent.class, name = "ASSIGNEE_CHANGED"),
		@JsonSubTypes.Type(value = StatusChangedEvent.class, name = "STATUS_CHANGED"),
		@JsonSubTypes.Type(value = BugReportClosedEvent.class, name = "BUG_REPORT_CLOSED")
})
public sealed interface IBugReportEvent permits AssigneeChangedEvent, StatusChangedEvent, BugReportClosedEvent {
	String reportTitle();
}
