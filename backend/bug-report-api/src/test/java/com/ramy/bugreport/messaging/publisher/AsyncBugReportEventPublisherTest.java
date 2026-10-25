package com.ramy.bugreport.messaging.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.ramy.bugreport.messaging.event.AssigneeChangedEvent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.ObjectMapper;
import jakarta.jms.Message;

class AsyncBugReportEventPublisherTest {

    private final AssigneeChangedEvent event = new AssigneeChangedEvent(
            java.util.UUID.randomUUID(), "Developer", "developer@example.com", "Report title");
    private final JmsTemplate template = mock(JmsTemplate.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final TransactionTemplate transaction = new TransactionTemplate(new TestTransactionManager());
    private AnnotationConfigApplicationContext context;
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test", Map.of("messaging.destinations.bug-report-event", "bug-report-event")));
        context.registerBean(JmsTemplate.class, () -> template);
        context.registerBean(ObjectMapper.class, () -> objectMapper);
        // Spring Boot normally supplies this factory through transaction auto-configuration.
        context.registerBean(TransactionalEventListenerFactory.class);
        context.register(BugReportPublishingConfig.class, BugReportEventPublisher.class);
        context.refresh();
        executor = context.getBean("bugReportPublisherExecutor", ThreadPoolTaskExecutor.class);
        // Ignore lifecycle callbacks invoked when Spring initializes the JMS template bean.
        clearInvocations(template, objectMapper);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        context.close();
    }

    @Test
    void committedEventCarriesRequestIdThroughWorkerAndJmsWithoutLeakingIntoNextTask() throws Exception {
        String requestId = java.util.UUID.randomUUID().toString();
        AtomicReference<String> workerRequestId = new AtomicReference<>();
        Message message = mock(Message.class);
        when(objectMapper.writeValueAsString(event)).thenReturn("payload");
        doAnswer(invocation -> {
            workerRequestId.set(MDC.get("requestId"));
            invocation.getArgument(2, MessagePostProcessor.class).postProcessMessage(message);
            return null;
        }).when(template).convertAndSend(eq("bug-report-event"), eq("payload"), any(MessagePostProcessor.class));
        MDC.put("requestId", requestId);
        transaction.executeWithoutResult(status -> context.publishEvent(event));
        MDC.remove("requestId");
        // Inspect the worker directly, bypassing the decorator so it cannot mask a leaked value.
        assertThat(executor.getThreadPoolExecutor().submit(() -> MDC.get("requestId")).get(2, TimeUnit.SECONDS))
                .isNull();
        assertThat(workerRequestId.get()).isEqualTo(requestId);
        verify(message).setStringProperty("requestId", requestId);
        verify(template).convertAndSend(eq("bug-report-event"), eq("payload"), any(MessagePostProcessor.class));
    }

    @Test
    void commitsAndReturnsWhileSendIsStillRunningOnWorkerThread() throws Exception {
        CountDownLatch sendStarted = new CountDownLatch(1);
        CountDownLatch releaseSend = new CountDownLatch(1);
        AtomicReference<String> senderThread = new AtomicReference<>();
        when(objectMapper.writeValueAsString(event)).thenReturn("payload");
        doAnswer(invocation -> {
            senderThread.set(Thread.currentThread().getName());
            sendStarted.countDown();
            if (!releaseSend.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Send was not released by the test");
            }
            return null;
        }).when(template).convertAndSend(eq("bug-report-event"), eq("payload"), any(MessagePostProcessor.class));

        var requestThread = Executors.newSingleThreadExecutor();
        try {
            var response = requestThread.submit(() -> transaction.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(template, objectMapper);
            }));

            // The transaction finishes even though the JMS call is deliberately blocked.
            response.get(2, TimeUnit.SECONDS);
            assertThat(sendStarted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(releaseSend.getCount()).isEqualTo(1);
            assertThat(senderThread.get()).startsWith("bug-report-publisher-");
        } finally {
            releaseSend.countDown();
            requestThread.shutdownNow();
        }
        executor.submit(() -> { }).get(2, TimeUnit.SECONDS);
        verify(template).convertAndSend(eq("bug-report-event"), eq("payload"), any(MessagePostProcessor.class));
    }

    @Test
    void rollbackDoesNotScheduleNotification() {
        transaction.executeWithoutResult(status -> {
            context.publishEvent(event);
            status.setRollbackOnly();
        });

        assertThat(executor.getThreadPoolExecutor().getTaskCount()).isZero();
        verifyNoInteractions(template, objectMapper);
    }

    @Test
    void fullQueueLogsAndDropsSubmissionWithoutRunningItOnCaller() throws Exception {
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        Logger logger = (Logger) LoggerFactory.getLogger(BugReportPublishingConfig.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(logs);
        try {
            executor.execute(() -> {
                workerStarted.countDown();
                try {
                    releaseWorker.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertThat(workerStarted.await(2, TimeUnit.SECONDS)).isTrue();
            for (int task = 0; task < 100; task++) {
                executor.execute(() -> { });
            }

            transaction.executeWithoutResult(status -> context.publishEvent(event));

            assertThat(executor.getQueueSize()).isEqualTo(100);
            assertThat(logs.list).singleElement().satisfies(entry -> {
                assertThat(entry.getLevel()).isEqualTo(Level.ERROR);
                assertThat(entry.getFormattedMessage()).contains("Publisher task rejected", "notification dropped");
            });
            verifyNoInteractions(template, objectMapper);
            var rejectedFuture = executor.submit(() -> { });
            assertThat(rejectedFuture.isCancelled()).isTrue();
        } finally {
            releaseWorker.countDown();
            logger.detachAppender(logs);
            logger.setLevel(previousLevel);
            logs.stop();
        }
    }

    /** Real Spring transaction callbacks without a database; persistence is outside these tests' scope. */
    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) { }

        @Override
        protected void doCommit(DefaultTransactionStatus status) { }

        @Override
        protected void doRollback(DefaultTransactionStatus status) { }
    }
}
