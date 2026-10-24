package com.ramy.bugreport.messaging.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class BugReportPublishingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BugReportPublishingConfig.class);

    @Test
    void retainsExistingDefaultsWhenPropertiesAreAbsent() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            var executor = context.getBean("bugReportPublisherExecutor", ThreadPoolTaskExecutor.class);
            assertThat(executor.getCorePoolSize()).isEqualTo(1);
            assertThat(executor.getMaxPoolSize()).isEqualTo(1);
            assertThat(executor.getQueueCapacity()).isEqualTo(100);
        });
    }

    @ParameterizedTest
    @CsvSource({"2, 4, 7", "1, 3, 0"})
    void appliesConfiguredValues(int corePoolSize, int maxPoolSize, int queueCapacity) {
        withPoolSettings(corePoolSize, maxPoolSize, queueCapacity).run(context -> {
            assertThat(context).hasNotFailed();
            var executor = context.getBean("bugReportPublisherExecutor", ThreadPoolTaskExecutor.class);
            assertThat(executor.getCorePoolSize()).isEqualTo(corePoolSize);
            assertThat(executor.getMaxPoolSize()).isEqualTo(maxPoolSize);
            assertThat(executor.getQueueCapacity()).isEqualTo(queueCapacity);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1, 100, messaging.publisher.core-pool-size must be positive",
            "-1, 1, 100, messaging.publisher.core-pool-size must be positive",
            "2, 1, 100, messaging.publisher.max-pool-size must be at least core-pool-size",
            "1, 0, 100, messaging.publisher.max-pool-size must be at least core-pool-size",
            "1, 1, -1, messaging.publisher.queue-capacity must not be negative"
    })
    void invalidSettingsPreventStartup(int corePoolSize, int maxPoolSize, int queueCapacity, String message) {
        withPoolSettings(corePoolSize, maxPoolSize, queueCapacity).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage(message);
        });
    }

    private ApplicationContextRunner withPoolSettings(int corePoolSize, int maxPoolSize, int queueCapacity) {
        return contextRunner.withPropertyValues(
                "messaging.publisher.core-pool-size=" + corePoolSize,
                "messaging.publisher.max-pool-size=" + maxPoolSize,
                "messaging.publisher.queue-capacity=" + queueCapacity);
    }
}
