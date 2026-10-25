package com.ramy.bugreport.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.ramy.bugreport.messaging.event.StatusChangedEvent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.ObjectMapper;

class BugReportEventConsumerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final BugReportEventConsumer consumer = new BugReportEventConsumer(mapper, sender, "no-reply@example.com");
    private final UUID reportId = UUID.randomUUID();
    private final String requestId = UUID.randomUUID().toString();
    private final Logger logger = (Logger) LoggerFactory.getLogger(BugReportEventConsumer.class);
    private Level previousLevel;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        logs = new ListAppender<>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logger.setLevel(previousLevel);
        logs.stop();
        MDC.clear();
    }

    @Test
    void mailAcceptancePreservesRecipientsAndCarriesRequestIdWithoutLoggingPrivateContent() {
        MDC.put("requestId", "outer-context");
        consumer.onBugReportEvent(payload("assignee@example.com", "reporter@example.com"), requestId);

        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("reporter@example.com", "assignee@example.com");
        assertThat(message.getValue().getSubject()).contains("Private title");
        assertSafeEntry(Level.INFO, "event=emailAccepted reportId=" + reportId);
        assertThat(logs.list.getFirst().getMDCPropertyMap()).containsEntry("requestId", requestId);
        assertThat(MDC.get("requestId")).isEqualTo("outer-context");
    }

    @Test
    void noRecipientsSkipsMailWithoutLoggingTitle() {
        consumer.onBugReportEvent(payload(null, null), requestId);
        verifyNoInteractions(sender);
        assertSafeEntry(Level.INFO, "event=emailSkipped reason=noRecipients reportId=" + reportId);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void mailFailureStillPropagatesButDoesNotExposeOriginalException() {
        doThrow(new MailSendException("private failure for assignee@example.com"))
                .when(sender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> consumer.onBugReportEvent(payload("assignee@example.com", null), requestId))
                .isInstanceOf(NotificationProcessingException.class).hasNoCause()
                .hasMessage("Notification processing failed");
        assertSafeEntry(Level.ERROR, "event=notificationFailed stage=mail reportId=" + reportId
                + " exceptionType=MailSendException");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void malformedMessageFailsWithoutEchoingPayloadAndIgnoresForgedCorrelation() {
        assertThatThrownBy(() -> consumer.onBugReportEvent("Private title and malformed JSON", "forged\nentry"))
                .isInstanceOf(NotificationProcessingException.class).hasNoCause();
        verifyNoInteractions(sender);
        assertThat(logs.list).singleElement().satisfies(entry -> {
            assertThat(entry.getLevel()).isEqualTo(Level.ERROR);
            assertThat(entry.getFormattedMessage()).contains("stage=decode").doesNotContain("Private title", "forged");
            assertThat(entry.getThrowableProxy()).isNull();
            assertThat(UUID.fromString(entry.getMDCPropertyMap().get("requestId"))).isNotNull();
        });
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void olderQueuedMessagesWithoutReportIdOrRequestIdRemainReadable() {
        consumer.onBugReportEvent("""
                {"eventType":"STATUS_CHANGED","reportTitle":"Private title",
                 "assigneeEmail":null,"reporterEmail":"reporter@example.com"}
                """, null);
        verify(sender).send(any(SimpleMailMessage.class));
        assertSafeEntry(Level.INFO, "event=emailAccepted reportId=null");
        assertThat(UUID.fromString(logs.list.getFirst().getMDCPropertyMap().get("requestId"))).isNotNull();
    }

    private String payload(String assigneeEmail, String reporterEmail) {
        return mapper.writeValueAsString(new StatusChangedEvent(reportId, "Private title\nwith newline", assigneeEmail, reporterEmail));
    }

    private void assertSafeEntry(Level level, String message) {
        assertThat(logs.list).singleElement().satisfies(entry -> {
            assertThat(entry.getLevel()).isEqualTo(level);
            assertThat(entry.getFormattedMessage()).isEqualTo(message)
                    .doesNotContain("Private title", "assignee@example.com", "reporter@example.com", "private failure");
            assertThat(entry.getThrowableProxy()).isNull();
        });
    }
}
