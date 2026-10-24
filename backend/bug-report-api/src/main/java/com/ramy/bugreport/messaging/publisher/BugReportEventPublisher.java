package com.ramy.bugreport.messaging.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ramy.bugreport.messaging.event.IBugReportEvent;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Forwards bug report events to the message queue (Artemis via JMS). The queue name comes from the property
 * {@code messaging.destinations.bug-report-event}.
 */
@Component
public class BugReportEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BugReportEventPublisher.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MILLIS = 1000;

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
     * that were rolled back. Makes up to three send attempts with a one-second pause between failures.
     * Failures are logged without propagating them; the already committed change is not undone.
     * Retries run on the calling thread and are not preserved across application restarts.
     *
     * @param event the event published through Spring's {@code ApplicationEventPublisher}
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBugReportEvent(IBugReportEvent event) {
        String eventType = event.getClass().getSimpleName();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            logger.error("Cannot serialize {}; notification was not sent (exceptionType={})",
                    eventType, exception.getClass().getSimpleName());
            return;
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                template.convertAndSend(bugReportEventDestination, payload);
                logger.info("Send attempt {}/{} succeeded for {}", attempt, MAX_ATTEMPTS, eventType);
                return;
            } catch (JmsException exception) {
                // Log exception types, not messages or payloads, which may contain recipient details.
                if (attempt == MAX_ATTEMPTS) {
                    logger.error("Send attempt {}/{} failed for {}; giving up (exceptionType={})",
                            attempt, MAX_ATTEMPTS, eventType, exception.getClass().getSimpleName());
                    return;
                }
                logger.warn("Send attempt {}/{} failed for {}; retrying in {} ms (exceptionType={})",
                        attempt, MAX_ATTEMPTS, eventType, RETRY_DELAY_MILLIS,
                        exception.getClass().getSimpleName());
            }

            try {
                Thread.sleep(RETRY_DELAY_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                logger.error("Retries interrupted for {}; notification delivery stopped", eventType);
                return;
            }
        }
    }

}
