package com.ramy.bugreport.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.adapter.ListenerExecutionFailedException;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.Session;

class BugReportConsumerConfigTest {
    @Test
    void retainsBootTransactionDefaultsAndAvoidsDuplicateOrPrivateFrameworkErrors() {
        Logger logger = (Logger) LoggerFactory.getLogger(BugReportConsumerConfig.class);
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        logger.addAppender(logs);
        try {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(JmsAutoConfiguration.class))
                    .withUserConfiguration(BugReportConsumerConfig.class)
                    .withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        var factory = context.getBean("jmsListenerContainerFactory", DefaultJmsListenerContainerFactory.class);
                        var endpoint = new SimpleJmsListenerEndpoint();
                        endpoint.setDestination("bug-report-event");
                        endpoint.setMessageListener(message -> { });
                        var container = factory.createListenerContainer(endpoint);
                        assertThat(container.isSessionTransacted()).isTrue();
                        // Exercise local rollback without opening a broker connection.
                        container.setAcceptMessagesWhileStopping(true);
                        container.setMessageListener((jakarta.jms.MessageListener) message -> {
                            throw new NotificationProcessingException();
                        });
                        Session session = mock(Session.class);
                        when(session.getTransacted()).thenReturn(true);
                        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                                container, "doExecuteListener", session, mock(Message.class)))
                                .isInstanceOf(NotificationProcessingException.class);
                        verify(session).rollback();
                        var handler = container.getErrorHandler();
                        assertThat(handler).isNotNull();
                        handler.handleError(new ListenerExecutionFailedException("wrapper", new NotificationProcessingException()));
                        assertThat(logs.list).isEmpty();
                        handler.handleError(new IllegalStateException("private payload"));
                        assertThat(logs.list).singleElement().satisfies(entry -> {
                            assertThat(entry.getFormattedMessage())
                                    .isEqualTo("event=notificationListenerFailed exceptionType=IllegalStateException");
                            assertThat(entry.getThrowableProxy()).isNull();
                        });
                    });
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }
}
