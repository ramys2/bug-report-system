package com.ramy.bugreport.messaging.publisher;

import java.util.concurrent.Future;

import com.ramy.bugreport.logging.RequestIdTaskDecorator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/** Keeps notification delivery off request threads and bounds its in-memory backlog. */
@Configuration
@EnableAsync
public class BugReportPublishingConfig {

    private static final Logger logger = LoggerFactory.getLogger(BugReportPublishingConfig.class);

    @Bean
    public ThreadPoolTaskExecutor bugReportPublisherExecutor(
            @Value("${messaging.publisher.core-pool-size:1}") int corePoolSize,
            @Value("${messaging.publisher.max-pool-size:1}") int maxPoolSize,
            @Value("${messaging.publisher.queue-capacity:100}") int queueCapacity) {
        Assert.isTrue(corePoolSize > 0, "messaging.publisher.core-pool-size must be positive");
        Assert.isTrue(maxPoolSize >= corePoolSize,
                "messaging.publisher.max-pool-size must be at least core-pool-size");
        Assert.isTrue(queueCapacity >= 0, "messaging.publisher.queue-capacity must not be negative");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("bug-report-publisher-");
        executor.setTaskDecorator(new RequestIdTaskDecorator());
        executor.setRejectedExecutionHandler((task, pool) -> {
            // Never run rejected work on the request thread. This executor serves void @Async methods.
            logger.error("Publisher task rejected: queue is full or executor is shutting down; notification dropped");
            Runnable originalTask = task instanceof RequestIdTaskDecorator.ContextTask contextTask
                    ? contextTask.task() : task;
            if (originalTask instanceof Future<?> future) {
                future.cancel(false);
            }
        });
        return executor;
    }
}
