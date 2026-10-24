package com.ramy.bugreport.messaging.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.status.Status;

class PublisherLoggingTest {

    @TempDir
    Path logDirectory;

    @Test
    void routesOnlyPublisherEntriesToSeparateLogFile() throws Exception {
        // Use an isolated context so the application's logging configuration stays untouched.
        LoggerContext context = new LoggerContext();
        try {
            context.setMDCAdapter(new LogbackMDCAdapter());
            context.putProperty("PUBLISHER_LOG_PATH", logDirectory.toString());
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(getClass().getResource("/logback-spring.xml"));
            context.start();

            var publisherLogger = context.getLogger(BugReportEventPublisher.class);
            publisherLogger.info("Publisher test entry");
            context.getLogger("com.ramy.bugreport.service.BugReportService").info("Other application entry");

            assertThat(publisherLogger.isAdditive()).isFalse();
            assertThat(context.getStatusManager().getCopyOfStatusList())
                    .noneMatch(status -> status.getEffectiveLevel() == Status.ERROR);
            assertThat(Files.readString(logDirectory.resolve("bug-report-publisher.log")))
                    .contains("Publisher test entry")
                    .doesNotContain("Other application entry");
        } finally {
            context.stop();
        }
    }
}
