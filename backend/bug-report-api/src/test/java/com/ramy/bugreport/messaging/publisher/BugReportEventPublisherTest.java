package com.ramy.bugreport.messaging.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

import com.ramy.bugreport.messaging.event.AssigneeChangedEvent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BugReportEventPublisherTest {

    private static final String DESTINATION = "bug-report-event";
    private static final String PAYLOAD = "{\"eventType\":\"ASSIGNEE_CHANGED\"}";
    private final AssigneeChangedEvent event = new AssigneeChangedEvent(
            "Developer", "developer@example.com", "Private report title");

    @Mock
    private JmsTemplate template;
    @Mock
    private ObjectMapper objectMapper;

    private BugReportEventPublisher publisher;
    private Logger logger;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        publisher = new BugReportEventPublisher(template, objectMapper, DESTINATION);
        logger = (Logger) LoggerFactory.getLogger(BugReportEventPublisher.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logger.setLevel(previousLevel);
        logs.stop();
    }

    @Test
    void stopsAfterFirstSuccessfulSend() {
        when(objectMapper.writeValueAsString(event)).thenReturn(PAYLOAD);

        publisher.onBugReportEvent(event);

        verify(template).convertAndSend(DESTINATION, PAYLOAD);
        assertThat(messages()).containsExactly("Send attempt 1/3 succeeded for AssigneeChangedEvent");
        assertThat(logs.list.getFirst().getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void retriesUntilThirdAttemptSucceedsAndSerializesOnlyOnce() {
        when(objectMapper.writeValueAsString(event)).thenReturn(PAYLOAD);
        doThrow(sendFailure()).doThrow(sendFailure()).doNothing()
                .when(template).convertAndSend(DESTINATION, PAYLOAD);

        publisher.onBugReportEvent(event);

        verify(template, times(3)).convertAndSend(DESTINATION, PAYLOAD);
        verify(objectMapper).writeValueAsString(event);
        assertThat(messages()).hasSize(3);
        assertThat(messages().get(0)).startsWith("Send attempt 1/3 failed");
        assertThat(messages().get(1)).startsWith("Send attempt 2/3 failed");
        assertThat(messages().get(2)).isEqualTo("Send attempt 3/3 succeeded for AssigneeChangedEvent");
        assertThat(logs.list).extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.WARN, Level.WARN, Level.INFO);
    }

    @Test
    void stopsAfterSecondAttemptSucceeds() {
        when(objectMapper.writeValueAsString(event)).thenReturn(PAYLOAD);
        doThrow(sendFailure()).doNothing().when(template).convertAndSend(DESTINATION, PAYLOAD);

        publisher.onBugReportEvent(event);

        verify(template, times(2)).convertAndSend(DESTINATION, PAYLOAD);
        assertThat(messages()).hasSize(2);
        assertThat(messages().getLast()).isEqualTo("Send attempt 2/3 succeeded for AssigneeChangedEvent");
    }

    @Test
    void logsAndContainsFailureAfterThreeAttemptsWithoutExposingEventData() {
        when(objectMapper.writeValueAsString(event)).thenReturn(PAYLOAD);
        doThrow(sendFailure()).when(template).convertAndSend(DESTINATION, PAYLOAD);

        publisher.onBugReportEvent(event);

        verify(template, times(3)).convertAndSend(DESTINATION, PAYLOAD);
        assertThat(messages()).hasSize(3);
        assertThat(messages().getLast()).startsWith("Send attempt 3/3 failed").contains("giving up");
        assertThat(logs.list).extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.WARN, Level.WARN, Level.ERROR);
        assertThat(String.join("\n", messages()))
                .doesNotContain(event.assigneeEmail(), event.reportTitle(), PAYLOAD);
        assertThat(logs.list).allSatisfy(entry -> assertThat(entry.getThrowableProxy()).isNull());
    }

    @Test
    void logsSerializationFailureWithoutSendingOrRetrying() {
        when(objectMapper.writeValueAsString(event)).thenThrow(mock(JacksonException.class));

        publisher.onBugReportEvent(event);

        verify(objectMapper).writeValueAsString(event);
        verifyNoInteractions(template);
        assertThat(messages()).hasSize(1);
        assertThat(messages().getFirst()).startsWith("Cannot serialize AssigneeChangedEvent");
        assertThat(logs.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void interruptionStopsRetriesAndPreservesInterruptFlag() {
        when(objectMapper.writeValueAsString(event)).thenReturn(PAYLOAD);
        doThrow(sendFailure()).when(template).convertAndSend(DESTINATION, PAYLOAD);

        Thread.currentThread().interrupt();
        try {
            publisher.onBugReportEvent(event);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(template).convertAndSend(DESTINATION, PAYLOAD);
            assertThat(messages()).hasSize(2);
            assertThat(messages().getLast()).startsWith("Retries interrupted");
        } finally {
            // Do not leak this test's interrupt status into other tests.
            Thread.interrupted();
        }
    }

    private JmsException sendFailure() {
        return new JmsException("Cannot send " + PAYLOAD + " to " + event.assigneeEmail()) { };
    }

    private List<String> messages() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
