package com.ramy.bugreport.logging;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/** Copies only the request ID into a publisher task and restores the worker's previous context. */
public class RequestIdTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable task) {
        return new ContextTask(task, MDC.get("requestId"));
    }

    /** Retains the original task so the executor can still cancel rejected futures. */
    public record ContextTask(Runnable task, String requestId) implements Runnable {
        @Override
        public void run() {
            String previousRequestId = MDC.get("requestId");
            try {
                if (requestId == null) {
                    MDC.remove("requestId");
                } else {
                    MDC.put("requestId", requestId);
                }
                task.run();
            } finally {
                if (previousRequestId == null) {
                    MDC.remove("requestId");
                } else {
                    MDC.put("requestId", previousRequestId);
                }
            }
        }
    }
}
