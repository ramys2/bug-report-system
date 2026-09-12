package com.ramy.bugreport.messaging.publisher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ramy.bugreport.messaging.event.IBugReportEvent;

import tools.jackson.databind.ObjectMapper;

@Component
public class BugReportEventPublisher {

	private final JmsTemplate template;
	private final ObjectMapper objectMapper;
	private final String bugReportEventDestination;

	public BugReportEventPublisher(
			JmsTemplate template,
			ObjectMapper objectMapper,
			@Value("${messaging.destinations.bug-report-event}")
			String bugReportEventDestination
		) {
		this.template = template;
		this.objectMapper = objectMapper;
		this.bugReportEventDestination = bugReportEventDestination;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onBugReportEvent(IBugReportEvent event) {
		template.convertAndSend(bugReportEventDestination, objectMapper.writeValueAsString(event));
	}

}
