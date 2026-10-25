package com.ramy.bugreport.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jms.ConnectionFactoryUnwrapper;
import org.springframework.boot.jms.autoconfigure.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import jakarta.jms.ConnectionFactory;

/** Keeps Boot's JMS settings and avoids logging a consumed message or its private exception text. */
@Configuration
public class BugReportConsumerConfig {
    private static final Logger logger = LoggerFactory.getLogger(BugReportConsumerConfig.class);

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            DefaultJmsListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        var factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, ConnectionFactoryUnwrapper.unwrapCaching(connectionFactory));
        factory.setErrorHandler(failure -> {
            // Listener exceptions are wrapped by Spring. The consumer already logged these failures.
            Throwable cause = failure;
            for (int depth = 0; cause != null && depth < 20; depth++, cause = cause.getCause()) {
                if (cause instanceof NotificationProcessingException) {
                    return;
                }
            }
            logger.error("event=notificationListenerFailed exceptionType={}", failure.getClass().getSimpleName());
        });
        return factory;
    }
}
