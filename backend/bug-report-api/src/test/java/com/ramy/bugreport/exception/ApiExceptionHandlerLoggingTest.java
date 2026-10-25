package com.ramy.bugreport.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.logging.RequestLoggingFilter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class ApiExceptionHandlerLoggingTest {
    @Test
    void databaseFailureGetsOneSafeCorrelatedErrorAndGeneric500Response() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger("com.ramy.bugreport");
        var logs = new ListAppender<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        logs.start();
        logger.addAppender(logs);
        try {
            var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                    .setControllerAdvice(new ApiExceptionHandler())
                    .addFilters(new RequestLoggingFilter(5000)).build();
            var response = mvc.perform(get("/api/database-test"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                    .andReturn().getResponse();
            assertThat(logs.list).singleElement().satisfies(entry -> {
                assertThat(entry.getLevel()).isEqualTo(Level.ERROR);
                assertThat(entry.getFormattedMessage()).isEqualTo(
                        "event=databaseOperationFailed operation=api exceptionType=DataAccessResourceFailureException");
                assertThat(entry.getMDCPropertyMap()).containsEntry("requestId", response.getHeader("X-Request-ID"));
                assertThat(entry.getThrowableProxy()).isNull();
            });
            assertThat(MDC.get("requestId")).isNull();
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    @RestController
    static class FailingController {
        @GetMapping("/api/database-test")
        public void fail() {
            throw new DataAccessResourceFailureException("private SQL parameter or connection detail");
        }
    }
}
