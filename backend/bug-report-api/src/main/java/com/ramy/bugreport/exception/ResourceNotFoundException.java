package com.ramy.bugreport.exception;

/** Thrown when a requested or referenced entity does not exist. Mapped to HTTP 404 by {@link ApiExceptionHandler}. */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 16777754540978053L;

	public ResourceNotFoundException(String message) {
        super(message);
    }

}
