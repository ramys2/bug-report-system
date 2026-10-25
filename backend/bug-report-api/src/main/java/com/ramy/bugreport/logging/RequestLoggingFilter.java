package com.ramy.bugreport.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Adds request correlation before security runs, and measures synchronous request processing. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final long slowRequestMillis;
    private final LongSupplier nanoTime;

    @Autowired
    public RequestLoggingFilter(@Value("${app.logging.slow-request-ms:5000}") long slowRequestMillis) {
        this(slowRequestMillis, System::nanoTime);
    }

    // A monotonic test clock allows threshold checks without delaying the test suite.
    RequestLoggingFilter(long slowRequestMillis, LongSupplier nanoTime) {
        Assert.isTrue(slowRequestMillis > 0, "app.logging.slow-request-ms must be positive");
        this.slowRequestMillis = slowRequestMillis;
        this.nanoTime = nanoTime;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String previousRequestId = MDC.get("requestId");
        String requestId = UUID.randomUUID().toString();
        // Generate our own ID rather than trusting an incoming header.
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        long started = nanoTime.getAsLong();
        try {
            chain.doFilter(request, response);
        } catch (DataAccessException exception) {
            // Database errors inside MVC are handled by ApiExceptionHandler. This covers filters.
            logger.error("event=databaseOperationFailed operation=requestFilter exceptionType={}",
                    exception.getClass().getSimpleName());
            throw exception;
        } finally {
            try {
                long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(nanoTime.getAsLong() - started);
                if (elapsedMillis >= slowRequestMillis) {
                    // Use the framework's route template, never a raw URL or query string.
                    Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                    logger.warn("event=slowRequest route={} elapsedMs={}",
                            route == null ? "unmapped" : route, elapsedMillis);
                }
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
