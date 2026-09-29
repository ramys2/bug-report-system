package com.ramy.bugreport.messaging.publisher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ramy.bugreport.messaging.event.IBugReportEvent;

import tools.jackson.databind.ObjectMapper;

/**
 * Forwards bug report events to the message queue (Artemis via JMS). The queue name comes from the property
 * {@code messaging.destinations.bug-report-event}.
 */
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

	/**
	 * Serializes the event to JSON and sends it to the queue.
	 *
	 * <p>Runs only after the transaction that published the event has committed, so no message is sent for changes
	 * that were rolled back. If sending fails, the already committed change is not undone.
	 *
	 * @param event the event published through Spring's {@code ApplicationEventPublisher}
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onBugReportEvent(IBugReportEvent event) {
		template.convertAndSend(bugReportEventDestination, objectMapper.writeValueAsString(event));
	}

}
