package com.ramy.bugreport.messaging.consumer;

import java.util.Arrays;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.ramy.bugreport.messaging.event.AssigneeChangedEvent;
import com.ramy.bugreport.messaging.event.BugReportClosedEvent;
import com.ramy.bugreport.messaging.event.IBugReportEvent;
import com.ramy.bugreport.messaging.event.StatusChangedEvent;

import tools.jackson.databind.ObjectMapper;

@Component
public class BugReportEventConsumer {

	private static final Logger logger = LoggerFactory.getLogger(BugReportEventConsumer.class);
	private static final String FROM_ADDRESS = "no-reply@bugreport.local";

	private final ObjectMapper objectMapper;
	private final JavaMailSender mailSender;

	public BugReportEventConsumer(ObjectMapper objectMapper, JavaMailSender mailSender) {
		this.objectMapper = objectMapper;
		this.mailSender = mailSender;
	}

	@JmsListener ( destination = "${messaging.destinations.bug-report-event}" )
	public void onBugReportEvent(String event) {
		var bugReportEvent = objectMapper.readValue(event, IBugReportEvent.class);
		var message = toMailMessage(bugReportEvent);

		if (message.getTo() == null || message.getTo().length == 0) {
			logger.info("Skipping email for '{}': no recipients", bugReportEvent.reportTitle());
			return;
		}

		mailSender.send(message);
	}

	private SimpleMailMessage toMailMessage(IBugReportEvent event) {
		return switch (event) {
			case AssigneeChangedEvent assigneeChanged -> mailMessage(
					"You have been assigned to '%s'".formatted(assigneeChanged.reportTitle()),
					"Hi %s, you have been assigned to the bug report '%s'.".formatted(
							assigneeChanged.assigneeName(), assigneeChanged.reportTitle()),
					assigneeChanged.assigneeEmail());
			case StatusChangedEvent statusChanged -> mailMessage(
					"Status changed for '%s'".formatted(statusChanged.reportTitle()),
					"The status of bug report '%s' has changed.".formatted(statusChanged.reportTitle()),
					statusChanged.reporterEmail(), statusChanged.assigneeEmail());
			case BugReportClosedEvent bugReportClosed -> mailMessage(
					"'%s' has been closed".formatted(bugReportClosed.reportTitle()),
					"The bug report '%s' has been closed.".formatted(bugReportClosed.reportTitle()),
					bugReportClosed.reporterEmail(), bugReportClosed.assigneeEmail());
		};
	}

	private SimpleMailMessage mailMessage(String subject, String text, String... recipients) {
		var message = new SimpleMailMessage();
		message.setFrom(FROM_ADDRESS);
		message.setSubject(subject);
		message.setText(text);
		message.setTo(Arrays.stream(recipients)
				.filter(Objects::nonNull)
				.toArray(String[]::new));
		return message;
	}

}
