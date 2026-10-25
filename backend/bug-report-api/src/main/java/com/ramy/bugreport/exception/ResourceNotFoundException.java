package com.ramy.bugreport.exception;

/** Thrown when a requested or referenced entity does not exist. Mapped to HTTP 404 by {@link ApiExceptionHandler}. */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 16777754540978053L;

    private final ErrorCode code;

    public ResourceNotFoundException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

}
