package com.ramy.bugreport.exception;

/** Thrown when registering an email address that is already in use. Mapped to HTTP 409 by {@link ApiExceptionHandler}. */
public class DuplicateEmailException extends RuntimeException {

    private static final long serialVersionUID = 6046659931344263352L;

	public DuplicateEmailException() {
        super("An account with this email address already exists.");
    }
}
