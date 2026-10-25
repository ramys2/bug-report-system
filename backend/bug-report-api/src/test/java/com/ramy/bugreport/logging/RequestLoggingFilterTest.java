package com.ramy.bugreport.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class RequestLoggingFilterTest {
    private final AtomicLong nanos = new AtomicLong();
    private final RequestLoggingFilter filter = new RequestLoggingFilter(5000, nanos::get);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reports/private-value");
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
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
        MDC.clear();
        logger.detachAppender(logs);
        logger.setLevel(previousLevel);
        logs.stop();
    }

    @Test
    void generatesAnIdIgnoresIncomingHeaderAndCleansUpContext() throws Exception {
        request.addHeader("X-Request-ID", "untrusted-input");
        filter.doFilter(request, response, (req, res) -> {
            String requestId = MDC.get("requestId");
            assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId);
            assertThat(response.getHeader("X-Request-ID")).isEqualTo(requestId).isNotEqualTo("untrusted-input");
        });
        assertThat(MDC.get("requestId")).isNull();
        assertThat(logs.list).isEmpty();
    }

    @Test
    void fiveSecondsWarnsWithRouteTemplateAndCorrelationWithoutRawUrl() throws Exception {
        request.setQueryString("token=private-secret");
        filter.doFilter(request, response, (req, res) -> {
            req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/reports/{reportId}");
            nanos.set(5_000_000_000L);
        });
        assertThat(logs.list).singleElement().satisfies(entry -> {
            assertThat(entry.getLevel()).isEqualTo(Level.WARN);
            assertThat(entry.getFormattedMessage()).contains("event=slowRequest", "elapsedMs=5000", "/api/reports/{reportId}")
                    .doesNotContain("private-value", "private-secret", "token=");
            assertThat(entry.getMDCPropertyMap()).containsEntry("requestId", response.getHeader("X-Request-ID"));
        });
    }

    @Test
    void requestsBelowThresholdDoNotWarn() throws Exception {
        filter.doFilter(request, response, (req, res) -> nanos.set(4_999_000_000L));
        assertThat(logs.list).isEmpty();
    }

    @Test
    void databaseFailurePropagatesWithSafeErrorAndRestoresPreviousContext() {
        MDC.put("requestId", "outer-context");
        var failure = new DataAccessResourceFailureException("private database value");
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> { throw failure; }))
                .isSameAs(failure);
        assertThat(MDC.get("requestId")).isEqualTo("outer-context");
        assertThat(logs.list).singleElement().satisfies(entry -> {
            assertThat(entry.getLevel()).isEqualTo(Level.ERROR);
            assertThat(entry.getFormattedMessage()).contains("event=databaseOperationFailed")
                    .doesNotContain(failure.getMessage());
            assertThat(entry.getThrowableProxy()).isNull();
        });
    }
}
