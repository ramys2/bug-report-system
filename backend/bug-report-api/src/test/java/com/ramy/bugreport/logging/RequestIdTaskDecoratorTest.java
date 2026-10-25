package com.ramy.bugreport.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestIdTaskDecoratorTest {
    private final RequestIdTaskDecorator decorator = new RequestIdTaskDecorator();

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void capturesAtSubmissionAndRestoresWorkerContextEvenWhenTaskFails() {
        MDC.put("requestId", "request-context");
        MDC.put("privateValue", "do-not-copy");
        Runnable task = decorator.decorate(() -> {
            assertThat(MDC.get("requestId")).isEqualTo("request-context");
            assertThat(MDC.get("privateValue")).isNull();
            throw new IllegalStateException("test failure");
        });
        MDC.clear();
        MDC.put("requestId", "worker-context");
        assertThatThrownBy(task::run).isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get("requestId")).isEqualTo("worker-context");
    }

    @Test
    void taskWithoutRequestCannotInheritAnOldRequestId() {
        Runnable task = decorator.decorate(() -> assertThat(MDC.get("requestId")).isNull());
        MDC.put("requestId", "old-worker-context");
        task.run();
        assertThat(MDC.get("requestId")).isEqualTo("old-worker-context");
    }
}
