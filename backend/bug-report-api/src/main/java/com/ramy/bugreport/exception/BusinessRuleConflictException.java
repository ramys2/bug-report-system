package com.ramy.bugreport.exception;

/**
 * Thrown when a request is well-formed but violates a business rule, e.g. changing a closed report
 * or demoting the last admin. Mapped to HTTP 409 by {@link ApiExceptionHandler}.
 */
public class BusinessRuleConflictException extends RuntimeException {

    private static final long serialVersionUID = 2145687505827373609L;

	public BusinessRuleConflictException(String message) {
        super(message);
    }
}
