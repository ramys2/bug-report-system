package com.ramy.bugreport.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.ramy.bugreport.messaging.event.AssigneeChangedEvent;

@Component 
public class BugReportEventConsumer {

	private static final Logger logger = LoggerFactory.getLogger(BugReportEventConsumer.class);

	@JmsListener ( destination = "${messaging.destinations.assignee-changed}" )
	public void onAssigneeChanged(String event) {
		logger.info("Received: %s", event);
	}

}