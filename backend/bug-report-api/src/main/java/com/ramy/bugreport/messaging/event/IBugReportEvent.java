package com.ramy.bugreport.messaging.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Notification about a change to a bug report. Published by {@code BugReportService} after the change is
 * committed, sent to the message queue as JSON, and turned into an email by {@code BugReportEventConsumer}.
 *
 * <p>The JSON carries an {@code eventType} property ({@code ASSIGNEE_CHANGED}, {@code STATUS_CHANGED} or
 * {@code BUG_REPORT_CLOSED}) that selects the implementing record when the message is read back. The interface is
 * sealed, so the consumer's {@code switch} covers every event type.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
		@JsonSubTypes.Type(value = AssigneeChangedEvent.class, name = "ASSIGNEE_CHANGED"),
		@JsonSubTypes.Type(value = StatusChangedEvent.class, name = "STATUS_CHANGED"),
		@JsonSubTypes.Type(value = BugReportClosedEvent.class, name = "BUG_REPORT_CLOSED")
})
public sealed interface IBugReportEvent permits AssigneeChangedEvent, StatusChangedEvent, BugReportClosedEvent {
	/** Title of the report the event is about. */
	String reportTitle();
}
