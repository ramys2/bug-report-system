package com.ramy.bugreport.messaging.publisher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ramy.bugreport.messaging.event.AssigneeChangedEvent;

@Component
public class BugReportEventPublisher {
	
	private final JmsTemplate template;
    private final String assigneeChangedDestination;
	
	public BugReportEventPublisher(
			JmsTemplate template,
			@Value("${messaging.destinations.assignee-changed}")
			String assigneeChangedDestination
		) {
		this.template = template;
		this.assigneeChangedDestination = assigneeChangedDestination;
	}
	
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendAssigneeChanged(AssigneeChangedEvent event) {
		var message = "Assignee in report '%s' changed to %s".formatted(event.reportTitle(), event.assigneeName());
		
		template.convertAndSend(
				assigneeChangedDestination,
				message
		);
	}

}
