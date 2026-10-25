package com.ramy.bugreport.exception;

/**
 * Thrown when a request is well-formed but violates a business rule, e.g. changing a closed report
 * or demoting the last admin. Mapped to HTTP 409 by {@link ApiExceptionHandler}.
 */
public class BusinessRuleConflictException extends RuntimeException {

    private static final long serialVersionUID = 2145687505827373609L;

    private final ErrorCode code;

    public BusinessRuleConflictException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
